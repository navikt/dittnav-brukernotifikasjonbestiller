package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object NokkelEventPairObjectMother {
    fun createANumberOfValidEvents(numberOfEvents: Int): List<Pair<NokkelIntern, String>> {
        return (1..numberOfEvents).map {
            NokkelIntern("ULID", it.toString(), "gruppering-$it", "fnr", "namespace", "appnavn","systembruker") to "event"
        }
    }

    fun createEventPair(eventId: String, systembruker: String): Pair<NokkelIntern, String> {
        return NokkelIntern("ULID", eventId, "gruppering-$eventId", "fnr", "namespace", "appnavn",systembruker) to "event"
    }
}