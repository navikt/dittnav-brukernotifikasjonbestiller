package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import java.time.Instant

object AvroInnboksObjectMother {

    private val defaultFodselsnummer = "12345678901"
    private val defaultGrupperingsid = "123"
    private val defaultTekst = "Dette er et innboks-event"
    private val defaultLink = "http://gyldig.url"
    private val defaultSikkerhetsnivaa = 4

    fun createInnboks(): Innboks {
        return createInnboks(defaultFodselsnummer, defaultGrupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithGrupperingsid(grupperingsid: String): Innboks {
        return createInnboks(defaultFodselsnummer, grupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboks(fodselsnummer, defaultGrupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithText(text: String): Innboks {
        return createInnboks(defaultFodselsnummer, defaultGrupperingsid, text, defaultLink, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithLink(link: String): Innboks {
        return createInnboks(defaultFodselsnummer, defaultGrupperingsid, defaultTekst, link, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Innboks {
        return createInnboks(defaultFodselsnummer, defaultGrupperingsid, defaultTekst, defaultLink, sikkerhetsnivaa)
    }

    private fun createInnboks(fodselsnummer: String, grupperingsid: String, tekst: String, link: String, sikkerhetsnivaa: Int): Innboks {
        return Innboks(
            Instant.now().toEpochMilli(),
            fodselsnummer,
            grupperingsid,
            tekst,
            link,
            sikkerhetsnivaa
        )
    }
}
