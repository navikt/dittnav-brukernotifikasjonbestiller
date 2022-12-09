package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import java.time.Instant

object AvroOppgaveInputObjectMother {

    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://gyldig.url"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()
    private val defaultEpostVarslingstekst: String = "eposttest"
    private val defaultEpostVarslingstittel: String = "eposttittel"
    private val defaultSmsVarslingstekst: String = "smstittel"

    fun createOppgaveInput(
        tekst: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        synligFremTil: Long? = defaultSynligFremTil,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
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

    fun createOppgaveInputWithText(text: String): OppgaveInput {
        return createOppgaveInput(
            text,
            defaultSikkerhetsnivaa,
            defaultEksternVarsling,
            defaultLink,
            defaultSynligFremTil,
            defaultPrefererteKanaler
        )
    }

    fun createOppgaveInputWithLink(link: String): OppgaveInput {
        return createOppgaveInput(
            defaultTekst,
            defaultSikkerhetsnivaa,
            defaultEksternVarsling,
            link,
            defaultSynligFremTil,
            defaultPrefererteKanaler
        )
    }

    fun createOppgaveInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): OppgaveInput {
        return createOppgaveInput(
            defaultTekst,
            sikkerhetsnivaa,
            defaultEksternVarsling,
            defaultLink,
            defaultSynligFremTil,
            defaultPrefererteKanaler
        )
    }

    fun createOppgaveInputWithSynligFremTil(synligFremTil: Long?): OppgaveInput {
        return createOppgaveInput(
            defaultTekst,
            defaultSikkerhetsnivaa,
            defaultEksternVarsling,
            defaultLink,
            synligFremTil,
            defaultPrefererteKanaler
        )
    }

    fun createOppgaveInputWithEksternVarslingAndPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): OppgaveInput {
        return createOppgaveInput(
            defaultTekst,
            defaultSikkerhetsnivaa,
            eksternVarsling,
            defaultLink,
            defaultSynligFremTil,
            prefererteKanaler
        )
    }
}
