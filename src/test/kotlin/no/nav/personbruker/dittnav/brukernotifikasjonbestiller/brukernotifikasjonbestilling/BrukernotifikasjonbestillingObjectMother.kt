package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime

object BrukernotifikasjonbestillingObjectMother {

    fun createBrukernotifikasjonbestilling(eventId: String, systembruker: String, eventtype: Eventtype): Brukernotifikasjonbestilling {
        return Brukernotifikasjonbestilling(
                eventId = eventId,
                systembruker = systembruker,
                eventtype = eventtype,
                prosesserttidspunkt = LocalDateTime.now()
        )
    }
}