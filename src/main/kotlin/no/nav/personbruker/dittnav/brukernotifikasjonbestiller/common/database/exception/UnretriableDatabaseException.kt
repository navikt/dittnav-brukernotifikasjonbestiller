package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.AbstractPersonbrukerException

class UnretriableDatabaseException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}