package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka

class RetriableKafkaException(message: String, cause: Throwable?) : IllegalStateException(message, cause) {
    constructor(message: String) : this(message, null)
}
