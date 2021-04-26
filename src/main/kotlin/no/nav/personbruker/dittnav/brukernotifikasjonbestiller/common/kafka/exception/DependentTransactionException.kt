package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.AbstractPersonbrukerException

class DependentTransactionException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause)

