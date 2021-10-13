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

    fun createBeskjedLegacy(): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacy(lopenummer: Int): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithText(text: String): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithLink(link: String): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjedLegacy(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithGrupperingsId(grupperingsid: String): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithSynligFremTil(synligFremTil: Long?): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, synligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Beskjed {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, prefererteKanaler)
    }

    private fun createBeskjedLegacy(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, synligFremTil: Long?, prefererteKanaler: List<String>): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                synligFremTil,
                fodselsnummer,
                grupperingsid,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
