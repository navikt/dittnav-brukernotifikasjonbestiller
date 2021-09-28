package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.legacy.BeskjedLegacy
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

    fun createBeskjedLegacy(): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacy(lopenummer: Int): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithText(text: String): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithLink(link: String): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithFodselsnummer(fodselsnummer: String): BeskjedLegacy {
        return createBeskjedLegacy(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithGrupperingsId(grupperingsid: String): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithSynligFremTil(synligFremTil: Long?): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, synligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedLegacy {
        return createBeskjedLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, prefererteKanaler)
    }

    private fun createBeskjedLegacy(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, synligFremTil: Long?, prefererteKanaler: List<String>): BeskjedLegacy {
        return BeskjedLegacy(
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
