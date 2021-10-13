package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object AvroNokkelInternObjectMother {

    fun createNokkelIntern(ulid: String, eventId: String, grupperingsId: String,fnr: String, namespace: String, appName: String, systembruker: String): NokkelIntern {
        return NokkelIntern(ulid, eventId, grupperingsId, fnr, namespace, appName, systembruker)
    }
}
