package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

