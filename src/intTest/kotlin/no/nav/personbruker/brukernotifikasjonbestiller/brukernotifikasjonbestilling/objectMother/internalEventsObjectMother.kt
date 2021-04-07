package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling.objectMother

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother

fun giveMeANumberOfInternalEvents(numberOfEvents: Int, eventId: String, systembruker: String): MutableMap<NokkelIntern, BeskjedIntern> {
    val result = mutableMapOf<NokkelIntern, BeskjedIntern>()

    for (i in 0 until numberOfEvents) {
        val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern(systembruker = "$systembruker-$i", eventId = "$eventId-$i", fnr = "$i")
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("$i")
        result.put(nokkelIntern, beskjedIntern)
    }
    return result
}