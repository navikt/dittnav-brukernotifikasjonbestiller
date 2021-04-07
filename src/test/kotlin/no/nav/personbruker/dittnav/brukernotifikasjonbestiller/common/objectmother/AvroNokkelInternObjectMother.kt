package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object AvroNokkelInternObjectMother {

    fun createNokkelIntern(systembruker: String, eventId: String, fnr: String): NokkelIntern {
        return NokkelIntern(systembruker, eventId, fnr)
    }

}