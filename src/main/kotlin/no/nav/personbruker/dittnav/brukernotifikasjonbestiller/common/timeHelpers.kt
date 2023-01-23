package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object CurrentTimeHelper {
    fun nowInEpochMillis() = Instant.now().toEpochMilli()
}

fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.of("UTC"))

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}
