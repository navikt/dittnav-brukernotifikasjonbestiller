package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception

class BatchUpdateException(message: String, cause: Throwable?) : RetriableDatabaseException(message, cause) {
    constructor(message: String) : this(message, null)
}