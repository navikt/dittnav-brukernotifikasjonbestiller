package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

object NokkelEventPairObjectMother {
    fun createANumberOfValidEvents(numberOfEvents: Int): List<Pair<NokkelIntern, String>> {
        return (1..numberOfEvents).map {
            NokkelIntern("systembruker", it.toString(),"fnr") to "event"
        }
    }

    fun createEventPair(eventId: String, systembruker: String): Pair<NokkelIntern, String> {
        return NokkelIntern(systembruker, eventId,"fnr") to "event"
    }
}
