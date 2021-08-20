package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroInnboksInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er et innboks-event"
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun giveMeANumberOfInternalInnboksEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, InnboksIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, InnboksIntern>>()
        for (i in 0 until numberOfEvents) {
            val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer)
            val innboksIntern = createInnboksInternWithGrupperingsId("$i")
            events.add(Pair(nokkelIntern, innboksIntern))
        }
        return events
    }

    fun createInnboksInternWithGrupperingsId(grupperingsid: String): InnboksIntern {
        return createInnboksIntern(defaultUlid, defaultTidspunkt, grupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa)
    }

    fun createInnboksIntern(ulid: String, tidspunkt: Long, grupperingsid: String, tekst: String, link: String, sikkerhetsnivaa: Int): InnboksIntern {
        return InnboksIntern(
            ulid,
            tidspunkt,
            grupperingsid,
            tekst,
            link,
            sikkerhetsnivaa
        )
    }
}
