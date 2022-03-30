package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import io.prometheus.client.Counter

object PrometheusMetricsCollector {

    const val NAMESPACE = "dittnav_brukernotifikasjonbestiller_consumer"

    const val EVENTS_SEEN_NAME = "kafka_events_seen"
    const val EVENTS_PROCESSED_NAME = "kafka_events_processed"
    const val EVENTS_FAILED_NAME = "kafka_events_failed"


    private val MESSAGES_SEEN: Counter = Counter.build()
        .name(EVENTS_SEEN_NAME)
        .namespace(NAMESPACE)
        .help("Events read since last startup")
        .labelNames("type", "producer")
        .register()

    private val MESSAGES_PROCESSED: Counter = Counter.build()
        .name(EVENTS_PROCESSED_NAME)
        .namespace(NAMESPACE)
        .help("Events successfully processed since last startup")
        .labelNames("type", "producer")
        .register()

    private val MESSAGES_FAILED: Counter = Counter.build()
        .name(EVENTS_FAILED_NAME)
        .namespace(NAMESPACE)
        .help("Events failed since last startup")
        .labelNames("type", "producer")
        .register()

    fun registerEventsSeen(count: Int, eventType: String, producer: String) {
        MESSAGES_SEEN.labels(eventType, producer).inc(count.toDouble())
    }

    fun registerEventsProcessed(count: Int, eventType: String, producer: String) {
        MESSAGES_PROCESSED.labels(eventType, producer).inc(count.toDouble())
    }

    fun registerEventsFailed(count: Int, eventType: String, producer: String) {
        MESSAGES_FAILED.labels(eventType, producer).inc(count.toDouble())
    }
}
