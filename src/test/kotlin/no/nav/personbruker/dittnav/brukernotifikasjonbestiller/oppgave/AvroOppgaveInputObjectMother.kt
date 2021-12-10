package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import java.time.Instant

object AvroOppgaveInputObjectMother {

    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createOppgaveInput(): OppgaveInput {
        return createOppgaveInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createOppgaveInputWithText(text: String): OppgaveInput {
        return createOppgaveInput(text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createOppgaveInputWithLink(link: String): OppgaveInput {
        return createOppgaveInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createOppgaveInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): OppgaveInput {
        return createOppgaveInput(defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createOppgaveInputWithSynligFremTil(synligFremTil: Long?): OppgaveInput {
        return createOppgaveInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, synligFremTil, defaultPrefererteKanaler)
    }

    fun createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): OppgaveInput {
        return createOppgaveInput(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultSynligFremTil, prefererteKanaler)
    }

    private fun createOppgaveInput(text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, synligFremTil: Long?, prefererteKanaler: List<String>): OppgaveInput {
        return OppgaveInput(
                Instant.now().toEpochMilli(),
                synligFremTil,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
