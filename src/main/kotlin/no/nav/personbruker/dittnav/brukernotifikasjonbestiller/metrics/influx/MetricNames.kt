package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx


private const val METRIC_NAMESPACE = "dittnav.brukernotifikasjonbestiller.v1"

const val KAFKA_EVENTS_PROCESSED = "$METRIC_NAMESPACE.processed"
const val KAFKA_RAPID_EVENTS_PROCESSED = "$METRIC_NAMESPACE.rapid.processed"
const val KAFKA_EVENTS_SEEN = "$METRIC_NAMESPACE.seen"
const val KAFKA_EVENTS_FAILED = "$METRIC_NAMESPACE.failed"
const val KAFKA_EVENTS_NOKKEL_NULL = "$METRIC_NAMESPACE.nokkelNull"
const val KAFKA_EVENTS_DUPLICATE_KEY = "$METRIC_NAMESPACE.duplicateKey"
const val KAFKA_EVENTS_PROCESSING_TIME = "$METRIC_NAMESPACE.processingTime"