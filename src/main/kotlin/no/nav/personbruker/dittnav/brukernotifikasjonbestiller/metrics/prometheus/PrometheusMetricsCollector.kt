package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.prometheus

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

object PrometheusMetricsCollector {

    const val NAMESPACE = "dittnav_brukernotifikasjonbestiller_consumer"

    const val SEEN_EVENTS_NAME = "kafka_events_seen"
    const val PROCESSED_EVENTS_NAME = "kafka_events_processed"
    const val LAST_SEEN_EVENTS_NAME = "kafka_event_type_last_seen"
    const val FAILED_EVENTS_NAME = "kafka_events_failed"
    const val DUPLICATE_KEY_EVENTS_NAME = "kafka_events_duplicate_key"
    const val NOKKEL_NULL_EVENTS_NAME = "kafka_events_nokkel_null"

    private val MESSAGES_SEEN: Counter = Counter.build()
            .name(SEEN_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events read since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGE_LAST_SEEN: Gauge = Gauge.build()
            .name(LAST_SEEN_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Last time event type was seen")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_PROCESSED: Counter = Counter.build()
            .name(PROCESSED_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events successfully processed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_FAILED: Counter = Counter.build()
            .name(FAILED_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events failed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_DUPLICATE_KEY: Counter = Counter.build()
            .name(DUPLICATE_KEY_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events skipped due to duplicate keys since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_NOKKEL_NULL: Counter = Counter.build()
            .name(NOKKEL_NULL_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events failed  due to Nokkel was null since last startup")
            .labelNames("type", "producer")
            .register()

    fun registerProcessedEvents(count: Int, topic: String, producer: String) {
        MESSAGES_PROCESSED.labels(topic, producer).inc(count.toDouble())
    }

    fun registerSeenEvents(count: Int, eventType: String, producer: String) {
        MESSAGES_SEEN.labels(eventType, producer).inc(count.toDouble())
        MESSAGE_LAST_SEEN.labels(eventType, producer).setToCurrentTime()
    }

    fun registerFailedEvents(count: Int, topic: String, producer: String) {
        MESSAGES_FAILED.labels(topic, producer).inc(count.toDouble())
    }

    fun registerDuplicateKeyEvents(count: Int, topic: String, producer: String) {
        MESSAGES_DUPLICATE_KEY.labels(topic, producer).inc(count.toDouble())
    }

    fun registerNokkelWasNullEvents(count: Int, topic: String, producer: String) {
        MESSAGES_NOKKEL_NULL.labels(topic, producer).inc(count.toDouble())
    }

}