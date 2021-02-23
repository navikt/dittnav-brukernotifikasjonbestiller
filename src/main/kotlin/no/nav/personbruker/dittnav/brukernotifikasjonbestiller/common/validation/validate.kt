package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun validateDateTime(dateAndTime: Long, fieldName: String): Long {
    try {
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateAndTime), ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy:HH:mm:ss.SS")
        val formattedDate = localDateTime.format(formatter)
        return dateAndTime
    } catch (exception: Exception) {
        val fve = FieldValidationException("Feltet " + fieldName + " kunne ikke konvertere fra LocalDateTime til UtcTimestamp.")
        fve.addContext("Exception", exception)
        throw fve
    }

}

