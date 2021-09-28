package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.legacy.OppgaveLegacy
import java.time.Instant

object AvroOppgaveLegacyObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultEksternVarsling = false
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createOppgaveLegacy(): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacy(lopenummer: Int): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithText(text: String): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithLink(link: String): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithFodselsnummer(fodselsnummer: String): OppgaveLegacy {
        return createOppgaveLegacy(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithGrupperingsId(grupperingsid: String): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): OppgaveLegacy {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, prefererteKanaler)
    }

    private fun createOppgaveLegacy(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, prefererteKanaler: List<String>): OppgaveLegacy {
        return OppgaveLegacy(
                Instant.now().toEpochMilli(),
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
