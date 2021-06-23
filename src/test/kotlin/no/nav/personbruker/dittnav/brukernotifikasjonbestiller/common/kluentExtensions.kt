package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import org.amshove.kluent.ExceptionResult
import org.amshove.kluent.shouldContain


infix fun <T : Throwable> ExceptionResult<T>.`with message containing`(theMessage: String) = this.exceptionMessage shouldContain (theMessage)
