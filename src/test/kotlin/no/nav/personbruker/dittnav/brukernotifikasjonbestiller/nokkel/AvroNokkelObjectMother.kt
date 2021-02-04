package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.Nokkel

object AvroNokkelObjectMother {

    fun createNokkelWithEventId(eventId: Int): Nokkel = Nokkel("dummySystembruker", eventId.toString())

}
