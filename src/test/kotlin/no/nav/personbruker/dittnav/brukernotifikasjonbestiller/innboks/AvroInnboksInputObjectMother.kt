package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import java.time.Instant

object AvroInnboksInputObjectMother {

    private val defaultTekst = "Dette er en innboks til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createInnboksInput(): InnboksInput {
        return createInnboksInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createInnboksInputWithText(text: String): InnboksInput {
        return createInnboksInput(text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createInnboksInputWithLink(link: String): InnboksInput {
        return createInnboksInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultPrefererteKanaler)
    }

    fun createInnboksInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): InnboksInput {
        return createInnboksInput(defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): InnboksInput {
        return createInnboksInput(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, prefererteKanaler)
    }

    private fun createInnboksInput(text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, prefererteKanaler: List<String>): InnboksInput {
        return InnboksInput(
                Instant.now().toEpochMilli(),
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
