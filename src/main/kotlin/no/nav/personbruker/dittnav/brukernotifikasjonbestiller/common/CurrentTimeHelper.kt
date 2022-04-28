package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import java.time.Instant

object CurrentTimeHelper {
    fun nowInEpochMillis() = Instant.now().toEpochMilli()
}
