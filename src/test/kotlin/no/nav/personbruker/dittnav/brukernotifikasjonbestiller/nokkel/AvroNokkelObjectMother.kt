package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.Nokkel

object AvroNokkelObjectMother {

    fun createNokkelWithEventId(eventId: String): Nokkel = Nokkel("dummySystembruker", eventId)

    fun createNokkelWithSystembruker(systembruker: String): Nokkel = Nokkel(systembruker, "1")

}
