package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime

data class Brukernotifikasjonbestilling(
        val eventId: String,
        val systembruker: String,
        val eventtype: Eventtype,
        val prosesserttidspunkt: LocalDateTime
)