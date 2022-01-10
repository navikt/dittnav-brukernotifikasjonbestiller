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
    private val defaultSmsVarslingstekst: String? = null

    fun createBeskjedInput(
        text: String = defaultTekst,
        link: String = defaultLink,
        eksternVarsling: Boolean = defaultEksternVarsling,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        synligFremTil: Long = defaultSynligFremTil,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        smsVarslingstekst: String? = defaultSmsVarslingstekst

    ): BeskjedInput {
        return createBeskjedInput(
            text,
            sikkerhetsnivaa,
            eksternVarsling,
            link,
            synligFremTil,
            prefererteKanaler,
            epostVarslingstekst,
            smsVarslingstekst
        )
    }

    fun createBeskjedInputWithText(text: String): BeskjedInput {
        return createBeskjedInput(text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedInputWithLink(link: String): BeskjedInput {
        return createBeskjedInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): BeskjedInput {
        return createBeskjedInput(defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedInputWithSynligFremTil(synligFremTil: Long?): BeskjedInput {
        return createBeskjedInput(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, synligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedInput {
        return createBeskjedInput(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultSynligFremTil, prefererteKanaler)
    }

    private fun createBeskjedInput(text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, synligFremTil: Long?, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, smsVarslingstekst: String? = null): BeskjedInput {
        return BeskjedInput(
                Instant.now().toEpochMilli(),
                synligFremTil,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler,
                epostVarslingstekst,
                smsVarslingstekst
        )
    }
}
