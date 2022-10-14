package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import java.time.Instant

object AvroInnboksInputObjectMother {

    private val defaultTekst = "Dette er en innboks til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://gyldig.url"
    private val defaultPrefererteKanaler = emptyList<String>()
    private val defaultEpostVarslingstekst: String = "eposttest"
    private val defaultEpostVarslingstittel: String = "eposttittel"
    private val defaultSmsVarslingstekst: String = "smstittel"

    fun createInnboksInput(
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): InnboksInput {
        return InnboksInput(
            Instant.now().toEpochMilli(),
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }

    fun createInnboksInputWithText(text: String): InnboksInput {
        return createInnboksInput(text = text)
    }

    fun createInnboksInputWithLink(link: String): InnboksInput {
        return createInnboksInput(link = link)
    }

    fun createInnboksInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): InnboksInput {
        return createInnboksInput(sikkerhetsnivaa = sikkerhetsnivaa)
    }

    fun createInnboksInputWithEksternVarslingAndPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): InnboksInput {
        return createInnboksInput(eksternVarsling = eksternVarsling, prefererteKanaler = prefererteKanaler)
    }
}
