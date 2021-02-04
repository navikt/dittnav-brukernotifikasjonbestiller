package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception

class UnretriableKafkaException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}
