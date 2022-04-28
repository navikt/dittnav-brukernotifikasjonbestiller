package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

class MetricsCollector(private val metricsReporter: MetricsReporter) {

    private val nokkelNullProducer = NamespaceAppName("Unknown", "NokkelWasNull")

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSession.() -> Unit) {
        val session = EventMetricsSession(eventType)
        block.invoke(session)
        val processingTime = session.timeElapsedSinceSessionStartNanos()

        if (session.getEventsSeen() > 0) {
            handleSeenEvents(session)
            handleProcessedEvents(session)
            handleFailedEvents(session)
            handleDuplicateEventKeys(session)
            handleNokkelWasNull(session)
            handleEventsProcessingTime(session, processingTime)
        }
    }

    private suspend fun handleSeenEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberSeen = session.getEventsSeen(producer)
            val eventTypeName = session.eventtype.toString()

            reportMetrics(KAFKA_EVENTS_SEEN, numberSeen, eventTypeName, producer)
            PrometheusMetricsCollector.registerEventsSeen(numberSeen, eventTypeName, producer.appName)
        }
    }

    private suspend fun handleProcessedEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberProcessed = session.getEventsProcessed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberProcessed > 0) {
                reportMetrics(KAFKA_EVENTS_PROCESSED, numberProcessed, eventTypeName, producer)
                PrometheusMetricsCollector.registerEventsProcessed(numberProcessed, eventTypeName, producer.appName)
            }
        }
    }

    private suspend fun handleFailedEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberFailed = session.getEventsFailed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberFailed > 0) {
                reportMetrics(KAFKA_EVENTS_FAILED, numberFailed, eventTypeName, producer)
                PrometheusMetricsCollector.registerEventsFailed(numberFailed, eventTypeName, producer.appName)
            }
        }
    }

    private suspend fun handleDuplicateEventKeys(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberDuplicateKeys = session.getDuplicateKeys(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberDuplicateKeys > 0) {
                reportMetrics(KAFKA_EVENTS_DUPLICATE_KEY, numberDuplicateKeys, eventTypeName, producer)
            }
        }
    }

    private suspend fun handleNokkelWasNull(session: EventMetricsSession) {
        val numberNokkelWasNull = session.getNokkelWasNull()
        val eventTypeName = session.eventtype.toString()

        if (numberNokkelWasNull > 0) {
            reportMetrics(KAFKA_EVENTS_NOKKEL_NULL, numberNokkelWasNull, eventTypeName, nokkelNullProducer)
        }
    }

    private suspend fun handleEventsProcessingTime(session: EventMetricsSession, processingTime: Long) {
        val metricsOverHead = session.timeElapsedSinceSessionStartNanos() - processingTime

        val fieldMap = listOf(
                "seen" to session.getEventsSeen(),
                "processed" to session.getEventsProcessed(),
                "failed" to session.getEventsFailed(),
                "processingTime" to processingTime,
                "metricsOverheadTime" to metricsOverHead
        ).toMap()

        val tagMap = listOf("eventType" to session.eventtype.toString()).toMap()

        metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, fieldMap, tagMap)
    }

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producer: NamespaceAppName) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producer))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: NamespaceAppName): Map<String, String> =
            listOf(
                "eventType" to eventType,
                "producer" to producer.appName,
                "producerNamespace" to producer.namespace,
                "topicSource" to TopicSource.AIVEN.name
            ).toMap()
}
