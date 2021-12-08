package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import java.time.Instant

object AvroOppgaveLegacyObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultEksternVarsling = false
    private val defaultPrefererteKanaler = emptyList<String>()

    fun createOppgaveLegacy(): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacy(lopenummer: Int): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, "$defaultTekst-$lopenummer", defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithText(text: String): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithLink(link: String): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithFodselsnummer(fodselsnummer: String): Oppgave {
        return createOppgaveLegacy(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithGrupperingsId(grupperingsid: String): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Oppgave {
        return createOppgaveLegacy(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, prefererteKanaler)
    }

    private fun createOppgaveLegacy(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, prefererteKanaler: List<String>): Oppgave {
        return Oppgave(
            Instant.now().toEpochMilli(),
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
