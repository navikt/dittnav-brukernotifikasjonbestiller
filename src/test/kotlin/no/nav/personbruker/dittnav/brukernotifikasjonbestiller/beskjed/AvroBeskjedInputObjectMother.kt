package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import java.time.Instant

object AvroBeskjedInputObjectMother {

    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()
    private val defaultEpostVarslingstekst: String? = null
    private val defaultEpostVarslingstittel: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createBeskjedInput(
        text: String = defaultTekst,
        link: String = defaultLink,
        eksternVarsling: Boolean = defaultEksternVarsling,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        synligFremTil: Long? = defaultSynligFremTil,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst

    ): BeskjedInput {
        return BeskjedInput(
            Instant.now().toEpochMilli(),
            synligFremTil,
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

    fun createBeskjedInputWithText(text: String): BeskjedInput {
        return createBeskjedInput(text = text)
    }

    fun createBeskjedInputWithLink(link: String): BeskjedInput {
        return createBeskjedInput(link = link)
    }

    fun createBeskjedInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): BeskjedInput {
        return createBeskjedInput(sikkerhetsnivaa = sikkerhetsnivaa)
    }

    fun createBeskjedInputWithSynligFremTil(synligFremTil: Long?): BeskjedInput {
        return createBeskjedInput(synligFremTil = synligFremTil)
    }

    fun createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedInput {
        return createBeskjedInput(eksternVarsling = eksternVarsling, prefererteKanaler = prefererteKanaler)
    }
}
