package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother

object InternalEventsObjectMother {

    fun giveMeANumberOfInternalEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableMap<NokkelIntern, BeskjedIntern> {
        val events = mutableMapOf<NokkelIntern, BeskjedIntern>()

        for (i in 0 until numberOfEvents) {
            events.put(
                    AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer),
                    AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("$i")
            )
        }
        return events
    }
}