package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import java.time.Instant

object InnboksTestData {

    fun innboksInput(
        tekst: String = "Dette er en innboks til bruker.",
        sikkerhetsnivaa: Int = 4,
        eksternVarsling: Boolean = true,
        link: String = "http://gyldig.url",
        prefererteKanaler: List<String> = emptyList(),
        epostVarslingstekst: String? = "eposttekst",
        epostVarslingstittel: String? = "eposttittel",
        smsVarslingstekst: String? = "smstekst",
    ): InnboksInput {
        return InnboksInput(
            Instant.now().toEpochMilli(),
            tekst,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }
}
