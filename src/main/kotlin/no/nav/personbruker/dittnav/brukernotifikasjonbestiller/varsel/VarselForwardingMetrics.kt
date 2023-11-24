package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.prometheus.client.Counter

object VarselForwardingMetrics {

    private const val NAMESPACE = "dittnav_brukernotifikasjonbestiller_consumer"

    private const val EVENTS_PROCESSED_NAME = "kafka_events_processed"
    private const val EVENTS_FORWARDED_NAME = "kafka_events_forwarded"
    private const val EVENTS_SCRAPPED_NAME = "kafka_events_scrapped"

    private val EVENTS_PROCESSED: Counter = Counter.build()
        .name(EVENTS_PROCESSED_NAME)
        .namespace(NAMESPACE)
        .help("Events read since last startup")
        .labelNames("type", "producer")
        .register()

    private val EVENTS_FORWARDED: Counter = Counter.build()
        .name(EVENTS_FORWARDED_NAME)
        .namespace(NAMESPACE)
        .help("Events successfully processed since last startup")
        .labelNames("type", "producer")
        .register()

    private val EVENTS_SCRAPPED: Counter = Counter.build()
        .name(EVENTS_SCRAPPED_NAME)
        .namespace(NAMESPACE)
        .help("Events failed since last startup")
        .labelNames("type", "producer")
        .register()

    fun registerEventProcessed(eventType: String, producer: String) {
        EVENTS_PROCESSED.labels(eventType, producer).inc()
    }

    fun registerEventForwarded(eventType: String, producer: String) {
        EVENTS_FORWARDED.labels(eventType, producer).inc()
    }

    fun registerEventScrapped(eventType: String, producer: String) {
        EVENTS_SCRAPPED.labels(eventType, producer).inc()
    }
}
