package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroStatusoppdateringInternObjectMother {
    private val defaultUlid = "54321"
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultStatusGlobal = "statusglobal"
    private val defaultStatusIntern = "statusintern"
    private val defaultSakstema = "sakstema"


    fun giveMeANumberOfInternalStatusoppdateringEvents(numberOfEvents: Int, eventId: String, systembruker: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, StatusoppdateringIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, StatusoppdateringIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer)
            val statusoppdateringIntern = createStatusoppdateringInternWithGrupperingsId("$i")
            events.add(Pair(nokkelIntern, statusoppdateringIntern))
        }
        return events
    }

    fun createStatusoppdateringInternWithGrupperingsId(grupperingsid: String): StatusoppdateringIntern {
        return createStatusoppdateringIntern(defaultUlid, defaultTidspunkt, grupperingsid, defaultLink, defaultSikkerhetsnivaa, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringIntern(ulid: String, tidspunkt: Long, grupperingsid: String, link: String, sikkerhetsnivaa: Int, statusGlobal: String, statusIntern: String, sakstema: String): StatusoppdateringIntern {
        return StatusoppdateringIntern(
                ulid,
                tidspunkt,
                grupperingsid,
                link,
                sikkerhetsnivaa,
                statusGlobal,
                statusIntern,
                sakstema
        )
    }
}
