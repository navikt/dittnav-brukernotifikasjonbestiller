package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy

object AvroNokkelLegacyObjectMother {

    fun createNokkelLegacyWithEventId(eventId: String): NokkelLegacy = NokkelLegacy("dummySystembruker", eventId)

    fun createNokkelLegacyWithEventIdAndSystembruker(eventId: String, systembruker: String): NokkelLegacy = NokkelLegacy(systembruker, eventId)

    fun createNokkelLegacyWithSystembruker(systembruker: String): NokkelLegacy = NokkelLegacy(systembruker, "1")

}
