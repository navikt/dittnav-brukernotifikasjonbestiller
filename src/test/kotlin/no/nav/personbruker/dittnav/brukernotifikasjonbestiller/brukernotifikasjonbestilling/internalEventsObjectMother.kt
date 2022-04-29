package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother

fun giveMeANumberOfInternalEvents(numberOfEvents: Int, eventId: String, systembruker: String): MutableList<Pair<NokkelIntern, BeskjedIntern>> {
    val result = mutableListOf<Pair<NokkelIntern, BeskjedIntern>>()

    for (i in 0 until numberOfEvents) {
        val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern(systembruker = "$systembruker-$i", eventId = "$eventId-$i", fnr = "$i", grupperingsId = "$i", appName = "$systembruker-$i-app", namespace = "namespace", ulid = "123")
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedIntern()
        result.add(Pair(nokkelIntern, beskjedIntern))
    }
    return result
}
