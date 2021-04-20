package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling.objectMother

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother

fun giveMeANumberOfInternalEvents(numberOfEvents: Int, eventId: String, systembruker: String): MutableList<Pair<NokkelIntern, BeskjedIntern>> {
    val result = mutableListOf<Pair<NokkelIntern, BeskjedIntern>>()

    for (i in 0 until numberOfEvents) {
        val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern(systembruker = "$systembruker-$i", eventId = "$eventId-$i", fnr = "$i")
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("$i")
        result.add(Pair(nokkelIntern, beskjedIntern))
    }
    return result
}