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
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultEpostVarslingstekst: String? = null
    private val defaultEpostVarslingstittel: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createOppgaveLegacy(
        fodselsnummer: String = defaultFodselsnr,
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        grupperingsid: String = defaultGrupperingsid,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        synligFremTil: Long? = defaultSynligFremTil,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): Oppgave {
        return Oppgave(
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
            epostVarslingstittel,
            smsVarslingstekst
        )
    }

    fun createOppgaveLegacy(lopenummer: Int): Oppgave {
        return createOppgaveLegacy(
            text = "$defaultTekst-$lopenummer",
            grupperingsid = "$defaultGrupperingsid-$lopenummer",
        )
    }

    fun createOppgaveLegacyWithText(text: String): Oppgave {
        return createOppgaveLegacy(
            text = text,
        )
    }

    fun createOppgaveLegacyWithLink(link: String): Oppgave {
        return createOppgaveLegacy(
            link = link,
        )
    }

    fun createOppgaveLegacyWithFodselsnummer(fodselsnummer: String): Oppgave {
        return createOppgaveLegacy(
            fodselsnummer = fodselsnummer,
        )
    }

    fun createOppgaveLegacyWithGrupperingsId(grupperingsid: String): Oppgave {
        return createOppgaveLegacy(
            grupperingsid = grupperingsid,
        )
    }

    fun createOppgaveLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgaveLegacy(
            sikkerhetsnivaa = sikkerhetsnivaa,
        )
    }

    fun createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): Oppgave {
        return createOppgaveLegacy(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }
}
