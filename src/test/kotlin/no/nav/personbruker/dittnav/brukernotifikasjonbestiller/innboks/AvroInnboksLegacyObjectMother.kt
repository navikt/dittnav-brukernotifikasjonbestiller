package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import java.time.Instant

object AvroInnboksLegacyObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er et innboks-event."
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultEksternVarsling = false
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createInnboksLegacy(): Innboks {
        return createInnboksLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacy(lopenummer: Int): Innboks {
        return createInnboksLegacy(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithText(text: String): Innboks {
        return createInnboksLegacy(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithLink(link: String): Innboks {
        return createInnboksLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, link, defaultGrupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboksLegacy(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithGrupperingsId(grupperingsid: String): Innboks {
        return createInnboksLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultLink, grupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Innboks {
        return createInnboksLegacy(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Innboks {
        return createInnboksLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, eksternVarsling,prefererteKanaler)
    }

    private fun createInnboksLegacy(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, link: String, grupperingsid: String, externVarsling: Boolean, preferefteKanaler: List<String>): Innboks {
        return Innboks(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                grupperingsid,
                text,
                link,
                sikkerhetsnivaa,
                externVarsling,
                preferefteKanaler
        )
    }
}
