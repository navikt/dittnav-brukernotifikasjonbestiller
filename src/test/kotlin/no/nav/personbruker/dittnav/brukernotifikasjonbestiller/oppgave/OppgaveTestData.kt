package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import java.time.Instant

object OppgaveTestData {

    fun oppgaveInput(
        tekst: String = "Dette er en oppgave til bruker.",
        sikkerhetsnivaa: Int = 4,
        eksternVarsling: Boolean = true,
        link: String = "http://gyldig.url",
        synligFremTil: Long? = Instant.now().toEpochMilli(),
        prefererteKanaler: List<String> = emptyList(),
        epostVarslingstekst: String? = "eposttekst",
        epostVarslingstittel: String? = "eposttittel",
        smsVarslingstekst: String? = "smstekst",
    ): OppgaveInput {
        return OppgaveInput(
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
