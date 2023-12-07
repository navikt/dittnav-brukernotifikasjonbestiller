package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import java.time.Instant

object TestData {

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

    fun innboksInput(
        tekst: String = "Dette er en innboks til bruker.",
        sikkerhetsnivaa: Int = 4,
        eksternVarsling: Boolean = true,
        link: String = "http://gyldig.url",
        prefererteKanaler: List<String> = emptyList(),
        epostVarslingstekst: String? = "eposttekst",
        epostVarslingstittel: String? = "eposttittel",
        smsVarslingstekst: String? = "smstekst",
    ): InnboksInput {
        return InnboksInput(
            Instant.now().toEpochMilli(),
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

    fun doneInput(tidspunkt: Long = Instant.now().toEpochMilli()) =
        DoneInput(
            tidspunkt
        )
}
