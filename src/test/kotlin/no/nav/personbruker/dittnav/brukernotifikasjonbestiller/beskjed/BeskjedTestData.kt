package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.tms.brukernotifikasjon.schemas.input.BeskjedInput
import java.time.Instant

object BeskjedTestData {

    fun beskjedInput(
        tekst: String? = "Dette er en beskjed til bruker.",
        link: String? = "http://gyldig.url",
        eksternVarsling: Boolean? = true,
        sikkerhetsnivaa: Int? = 4,
        synligFremTil: Long? = Instant.now().toEpochMilli(),
        prefererteKanaler: List<String>? = emptyList(),
        epostVarslingstekst: String? = "eposttekst",
        epostVarslingstittel: String? = "eposttittel",
        smsVarslingstekst: String? = "smstekst"
    ): BeskjedInput {
        return BeskjedInput(
            Instant.now().toEpochMilli(),
            synligFremTil,
            tekst,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }
}
