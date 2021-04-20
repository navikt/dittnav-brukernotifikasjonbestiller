package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception

class DuplicateEventException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}