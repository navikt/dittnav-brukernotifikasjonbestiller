package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import java.time.Instant

object BeskjedTestData {

    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://gyldig.url"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()
    private val defaultEpostVarslingstekst: String = "eposttest"
    private val defaultEpostVarslingstittel: String = "eposttittel"
    private val defaultSmsVarslingstekst: String = "smstittel"

    fun beskjedInput(
        tekst: String? = defaultTekst,
        link: String? = defaultLink,
        eksternVarsling: Boolean? = defaultEksternVarsling,
        sikkerhetsnivaa: Int? = defaultSikkerhetsnivaa,
        synligFremTil: Long? = defaultSynligFremTil,
        prefererteKanaler: List<String>? = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
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

    fun createBeskjedInputWithText(text: String): BeskjedInput {
        return beskjedInput(tekst = text)
    }

    fun createBeskjedInputWithLink(link: String): BeskjedInput {
        return beskjedInput(link = link)
    }

    fun createBeskjedInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): BeskjedInput {
        return beskjedInput(sikkerhetsnivaa = sikkerhetsnivaa)
    }

    fun createBeskjedInputWithSynligFremTil(synligFremTil: Long?): BeskjedInput {
        return beskjedInput(synligFremTil = synligFremTil)
    }

    fun createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedInput {
        return beskjedInput(eksternVarsling = eksternVarsling, prefererteKanaler = prefererteKanaler)
    }
}
