package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedLegacyObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()
    private val defaultEpostVarslingstekst: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createBeskjedLegacy(
        fodselsnummer: String = defaultFodselsnr,
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        grupperingsid: String = defaultGrupperingsid,
        synligFremTil: Long? = defaultSynligFremTil,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): Beskjed {
        return Beskjed(
            Instant.now().toEpochMilli(),
            synligFremTil,
            fodselsnummer,
            grupperingsid,
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            smsVarslingstekst
        )
    }

    fun createBeskjedLegacy(lopenummer: Int): Beskjed {
        return createBeskjedLegacy(text = "$defaultTekst-$lopenummer", grupperingsid = "$defaultGrupperingsid-$lopenummer")
    }

    fun createBeskjedLegacyWithText(text: String): Beskjed {
        return createBeskjedLegacy(text = text)
    }

    fun createBeskjedLegacyWithLink(link: String): Beskjed {
        return createBeskjedLegacy(link = link)
    }

    fun createBeskjedLegacyWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjedLegacy(fodselsnummer = fodselsnummer)
    }

    fun createBeskjedLegacyWithGrupperingsId(grupperingsid: String): Beskjed {
        return createBeskjedLegacy(grupperingsid = grupperingsid)
    }

    fun createBeskjedLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Beskjed {
        return createBeskjedLegacy(sikkerhetsnivaa = sikkerhetsnivaa)
    }

    fun createBeskjedLegacyWithSynligFremTil(synligFremTil: Long?): Beskjed {
        return createBeskjedLegacy(synligFremTil = synligFremTil)
    }

    fun createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Beskjed {
        return createBeskjedLegacy(eksternVarsling = eksternVarsling, prefererteKanaler = prefererteKanaler)
    }
}
