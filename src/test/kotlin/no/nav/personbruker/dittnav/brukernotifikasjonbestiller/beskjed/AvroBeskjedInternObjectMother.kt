package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import java.time.Instant

object AvroBeskjedInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun createBeskjedInternWithGrupperingsId(grupperingsid: String): BeskjedIntern {
        return createBeskjedIntern(defaultUlid, defaultTidspunkt, defaultSynligFremTil, grupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedIntern(ulid: String, tidspunkt: Long, synligFremTil: Long, grupperingsid: String, tekst: String, link: String, sikkerhetsnivaa: Int, eksternvarsling: Boolean): BeskjedIntern {
        return BeskjedIntern(
                ulid,
                tidspunkt,
                synligFremTil,
                grupperingsid,
                tekst,
                link,
                sikkerhetsnivaa,
                eksternvarsling
        )
    }
}