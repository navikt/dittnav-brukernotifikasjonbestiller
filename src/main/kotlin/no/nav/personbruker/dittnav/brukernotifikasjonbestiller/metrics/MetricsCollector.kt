package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.prometheus.PrometheusMetricsCollector
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import org.slf4j.LoggerFactory

class MetricsCollector(private val metricsReporter: MetricsReporter, private val nameScrubber: ProducerNameScrubber) {

    private val log = LoggerFactory.getLogger(MetricsCollector::class.java)

    private var reported = 0

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
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberSeen = session.getEventsSeen(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            reportMetrics(KAFKA_EVENTS_SEEN, numberSeen, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerSeenEvents(numberSeen, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleProcessedEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberProcessed = session.getEventsProcessed(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberProcessed > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_PROCESSED, numberProcessed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerProcessedEvents(numberProcessed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleFailedEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberFailed = session.getEventsFailed(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberFailed > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_FAILED, numberFailed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerFailedEvents(numberFailed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleDuplicateEventKeys(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberDuplicateKeys = session.getDuplicateKeys(systemUser)
            val eventTypeName = session.eventtype.toString()

            if (numberDuplicateKeys > 0) {
                val printableAlias = nameScrubber.getPublicAlias(systemUser)
                reportMetrics(KAFKA_EVENTS_DUPLICATE_KEY, numberDuplicateKeys, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerDuplicateKeyEvents(numberDuplicateKeys, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleNokkelWasNull(session: EventMetricsSession) {
        val numberNokkelWasNull = session.getNokkelWasNull()
        val eventTypeName = session.eventtype.toString()
        val printableAlias = "NokkelWasNull"

        if (numberNokkelWasNull > 0) {
            reportMetrics(KAFKA_EVENTS_NOKKEL_NULL, numberNokkelWasNull, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerNokkelWasNullEvents(numberNokkelWasNull, eventTypeName, printableAlias)
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

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producerAlias: String) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producerAlias))

        if (++reported % 100 == 0) {
            log.info("Has reported another 100 metrics to influxdb")
        }
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: String): Map<String, String> =
            listOf("eventType" to eventType, "producer" to producer).toMap()
}
