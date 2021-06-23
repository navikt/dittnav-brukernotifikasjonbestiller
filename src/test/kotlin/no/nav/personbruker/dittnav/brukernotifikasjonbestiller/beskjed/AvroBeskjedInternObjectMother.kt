package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
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
    private val defaultPrefererteKanaler = emptyList<String>()

    fun giveMeANumberOfInternalBeskjedEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, BeskjedIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, BeskjedIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer)
            val beskjedIntern = createBeskjedInternWithGrupperingsId("$i")
            events.add(Pair(nokkelIntern, beskjedIntern))
        }
        return events
    }

    fun createBeskjedInternWithGrupperingsId(grupperingsid: String): BeskjedIntern {
        return createBeskjedIntern(defaultUlid, defaultTidspunkt, defaultSynligFremTil, grupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createBeskjedIntern(ulid: String, tidspunkt: Long, synligFremTil: Long, grupperingsid: String, tekst: String, link: String, sikkerhetsnivaa: Int, eksternvarsling: Boolean, prefererteKanaler: List<String>): BeskjedIntern {
        return BeskjedIntern(
                ulid,
                tidspunkt,
                synligFremTil,
                grupperingsid,
                tekst,
                link,
                sikkerhetsnivaa,
                eksternvarsling,
                prefererteKanaler
        )
    }
}
