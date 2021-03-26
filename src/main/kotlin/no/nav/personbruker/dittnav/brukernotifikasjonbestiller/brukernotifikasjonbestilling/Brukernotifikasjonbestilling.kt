package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import java.time.LocalDateTime

data class Brukernotifikasjonbestilling(
        val eventid: String,
        val systembruker: String,
        val eventtype: String,
        val eventtidspunkt: LocalDateTime
)