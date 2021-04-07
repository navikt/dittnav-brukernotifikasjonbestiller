package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import java.time.LocalDateTime

data class Brukernotifikasjonbestilling(
        val eventId: String,
        val systembruker: String,
        val eventtype: String,
        val prosesserttidspunkt: LocalDateTime
)