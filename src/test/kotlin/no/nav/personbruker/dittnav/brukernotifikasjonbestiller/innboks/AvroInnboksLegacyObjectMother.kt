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
    private val defaultEpostVarslingstekst: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createInnboksLegacy(
        fodselsnummer: String = defaultFodselsnr,
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        link: String = defaultLink,
        grupperingsid: String = defaultGrupperingsid,
        eksternVarsling: Boolean = defaultEksternVarsling,
        preferefteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): Innboks {
        return Innboks(
            Instant.now().toEpochMilli(),
            fodselsnummer,
            grupperingsid,
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            preferefteKanaler,
            epostVarslingstekst,
            smsVarslingstekst
        )
    }

    fun createInnboksLegacy(lopenummer: Int): Innboks {
        return createInnboksLegacy(
            text = "$defaultTekst-$lopenummer",
            grupperingsid = "$defaultGrupperingsid-$lopenummer",
        )
    }

    fun createInnboksLegacyWithText(text: String): Innboks {
        return createInnboksLegacy(
            text = text,
        )
    }

    fun createInnboksLegacyWithLink(link: String): Innboks {
        return createInnboksLegacy(
            link = link,
        )
    }

    fun createInnboksLegacyWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboksLegacy(
            fodselsnummer = fodselsnummer,
        )
    }

    fun createInnboksLegacyWithGrupperingsId(grupperingsid: String): Innboks {
        return createInnboksLegacy(
            grupperingsid = grupperingsid,
        )
    }

    fun createInnboksLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Innboks {
        return createInnboksLegacy(
            sikkerhetsnivaa = sikkerhetsnivaa,
        )
    }

    fun createInnboksLegacyWithEksternVarslingAndPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): Innboks {
        return createInnboksLegacy(
            eksternVarsling = eksternVarsling,
            preferefteKanaler = prefererteKanaler
        )
    }
}
