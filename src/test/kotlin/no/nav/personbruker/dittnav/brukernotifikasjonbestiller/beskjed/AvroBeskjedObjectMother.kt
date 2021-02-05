package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    private fun createBeskjed(lopenummer: Int, fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                fodselsnummer,
                grupperingsid,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling
        )
    }

}
