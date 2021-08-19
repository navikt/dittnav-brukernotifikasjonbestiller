package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

data class BrukernotifikasjonKey(
        val eventId: String,
        val systembruker: String,
        val eventtype: Eventtype
)
