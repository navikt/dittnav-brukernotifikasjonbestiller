package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.Nokkel

object AvroNokkelLegacyObjectMother {

    fun createNokkelLegacyWithEventId(eventId: String): Nokkel = Nokkel("dummySystembruker", eventId)

    fun createNokkelLegacyWithEventIdAndSystembruker(eventId: String, systembruker: String): Nokkel = Nokkel(systembruker, eventId)

    fun createNokkelLegacyWithSystembruker(systembruker: String): Nokkel = Nokkel(systembruker, "1")

}
