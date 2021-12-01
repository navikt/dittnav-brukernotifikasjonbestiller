package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

class MetricsCollectorLegacy(private val metricsReporter: MetricsReporter, private val nameScrubber: ProducerNameScrubber) {

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSessionLegacy.() -> Unit) {
        val session = EventMetricsSessionLegacy(eventType)
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

    private suspend fun handleSeenEvents(session: EventMetricsSessionLegacy) {
        session.getUniqueProducer().forEach { systemUser ->
            val numberSeen = session.getEventsSeen(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            reportMetrics(KAFKA_EVENTS_SEEN, numberSeen, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleProcessedEvents(session: EventMetricsSessionLegacy) {
        session.getUniqueProducer().forEach { systemUser ->
            val numberProcessed = session.getEventsProcessed(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberProcessed > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_PROCESSED, numberProcessed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleFailedEvents(session: EventMetricsSessionLegacy) {
        session.getUniqueProducer().forEach { systemUser ->
            val numberFailed = session.getEventsFailed(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberFailed > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_FAILED, numberFailed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleDuplicateEventKeys(session: EventMetricsSessionLegacy) {
        session.getUniqueProducer().forEach { systemUser ->
            val numberDuplicateKeys = session.getDuplicateKeys(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberDuplicateKeys > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_DUPLICATE_KEY, numberDuplicateKeys, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleNokkelWasNull(session: EventMetricsSessionLegacy) {
        val numberNokkelWasNull = session.getNokkelWasNull()
        val eventTypeName = session.eventtype.toString()
        val printableAlias = "NokkelWasNull"

        if (numberNokkelWasNull > 0) {
            reportMetrics(KAFKA_EVENTS_NOKKEL_NULL, numberNokkelWasNull, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleEventsProcessingTime(session: EventMetricsSessionLegacy, processingTime: Long) {
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

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producerAlias: String) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producerAlias))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: String): Map<String, String> =
            listOf(
                "eventType" to eventType,
                "producer" to producer,
                "producerNamespace" to "Unknown",
                "topicSource" to TopicSource.ON_PREM.name
            ).toMap()
}
