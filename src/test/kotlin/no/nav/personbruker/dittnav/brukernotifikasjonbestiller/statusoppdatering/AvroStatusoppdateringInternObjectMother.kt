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
    private val defaultNamespace = "namespace"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultBehandlet = Instant.now().toEpochMilli()
    private val defaultStatusGlobal = "statusglobal"
    private val defaultStatusIntern = "statusintern"
    private val defaultSakstema = "sakstema"


    fun giveMeANumberOfInternalStatusoppdateringEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, StatusoppdateringIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, StatusoppdateringIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = createNokkelIntern("$eventId-$i", fodselsnummer, "$systembruker-$i")
            val statusoppdateringIntern = createStatusoppdateringIntern()
            events.add(Pair(nokkelIntern, statusoppdateringIntern))
        }
        return events
    }

    fun createNokkelIntern(eventId: String, fnr: String, systembruker: String): NokkelIntern {
        return AvroNokkelInternObjectMother.createNokkelIntern(defaultUlid, eventId, defaultGrupperingsid, fnr, defaultNamespace, "$systembruker-app", systembruker)
    }

    fun createStatusoppdateringIntern(): StatusoppdateringIntern {
        return createStatusoppdateringIntern(defaultTidspunkt, defaultBehandlet, defaultLink, defaultSikkerhetsnivaa, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringIntern(tidspunkt: Long, behandlet: Long, link: String, sikkerhetsnivaa: Int, statusGlobal: String, statusIntern: String, sakstema: String): StatusoppdateringIntern {
        return StatusoppdateringIntern(
            tidspunkt,
            behandlet,
            link,
            sikkerhetsnivaa,
            statusGlobal,
            statusIntern,
            sakstema
        )
    }
}
