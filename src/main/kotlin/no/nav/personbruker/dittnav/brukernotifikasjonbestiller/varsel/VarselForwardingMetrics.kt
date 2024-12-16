package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.prometheus.metrics.core.metrics.Counter

object VarselForwardingMetrics {

    private const val NAMESPACE = "dittnav_brukernotifikasjonbestiller"

    private const val EVENTS_PROCESSED_NAME = "${NAMESPACE}_kafka_events_processed"
    private const val EVENTS_FORWARDED_NAME = "${NAMESPACE}_kafka_events_forwarded"
    private const val EVENTS_SCRAPPED_NAME = "${NAMESPACE}_kafka_events_scrapped"

    private val EVENTS_PROCESSED: Counter = Counter.builder()
        .name(EVENTS_PROCESSED_NAME)
        .help("Events read since last startup")
        .labelNames("type", "producer")
        .register()

    private val EVENTS_FORWARDED: Counter = Counter.builder()
        .name(EVENTS_FORWARDED_NAME)
        .help("Events successfully processed since last startup")
        .labelNames("type", "producer")
        .register()

    private val EVENTS_SCRAPPED: Counter = Counter.builder()
        .name(EVENTS_SCRAPPED_NAME)
        .help("Events failed since last startup")
        .labelNames("type", "producer")
        .register()

    fun registerEventProcessed(eventType: String, producer: String) {
        EVENTS_PROCESSED.labelValues(eventType, producer).inc()
    }

    fun registerEventForwarded(eventType: String, producer: String) {
        EVENTS_FORWARDED.labelValues(eventType, producer).inc()
    }

    fun registerEventScrapped(eventType: String, producer: String) {
        EVENTS_SCRAPPED.labelValues(eventType, producer).inc()
    }
}
