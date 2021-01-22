package no.nav.personbruker.brukernotifikasjonbestiller.common

import java.lang.Exception

class ConsumeEventException(message: String, cause: Throwable) : Exception(message, cause)
