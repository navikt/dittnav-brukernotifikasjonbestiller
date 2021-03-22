package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.AbstractPersonbrukerException

class RetriableKafkaException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}

