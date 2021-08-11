package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class DoneKey(
        val eventId: String,
        val systembruker: String,
        val eventtype: Eventtype,
        val fodselsnummer: String
)