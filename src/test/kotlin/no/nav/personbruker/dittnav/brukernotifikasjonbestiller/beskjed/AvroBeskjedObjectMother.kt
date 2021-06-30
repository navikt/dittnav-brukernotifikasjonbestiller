package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createBeskjed(): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithLink(link: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithGrupperingsId(grupperingsid: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithSynligFremTil(synligFremTil: Long?): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, synligFremTil, defaultPrefererteKanaler)
    }

    fun createBeskjedWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil, prefererteKanaler)
    }

    private fun createBeskjed(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, synligFremTil: Long?, prefererteKanaler: List<String>): Beskjed {
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
