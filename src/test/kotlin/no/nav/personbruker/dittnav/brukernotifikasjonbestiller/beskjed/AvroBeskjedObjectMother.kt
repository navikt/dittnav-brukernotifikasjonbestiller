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

    fun createBeskjed(): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil)
    }

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultSynligFremTil)
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil)
    }

    fun createBeskjedWithLink(link: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultSynligFremTil)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil)
    }

    fun createBeskjedWithGrupperingsId(grupperingsid: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultSynligFremTil)
    }

    fun createBeskjedWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultSynligFremTil)
    }

    fun createBeskjedWithSynligFremTil(synligFremTil: Long?): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, synligFremTil)
    }

    private fun createBeskjed(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, synligFremTil: Long?): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                synligFremTil,
                fodselsnummer,
                grupperingsid,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling
        )
    }
}
