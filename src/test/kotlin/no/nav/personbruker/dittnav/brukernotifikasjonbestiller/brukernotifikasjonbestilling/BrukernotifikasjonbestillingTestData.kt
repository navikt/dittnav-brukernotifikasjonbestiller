package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object BrukernotifikasjonbestillingTestData {

    fun brukernotifikasjonbestilling(eventId: String, systembruker: String, eventtype: Eventtype, fodselsnummer: String): Brukernotifikasjonbestilling {
        return Brukernotifikasjonbestilling(
                eventId = eventId,
                systembruker = systembruker,
                eventtype = eventtype,
                prosesserttidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                fodselsnummer = fodselsnummer
        )
    }
}
