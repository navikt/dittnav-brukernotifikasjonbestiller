package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object NokkelEventPairObjectMother {
    fun createANumberOfValidEvents(numberOfEvents: Int): List<Pair<NokkelIntern, String>> {
        return (1..numberOfEvents).map {
            NokkelIntern("systembruker", it.toString(),"fnr") to "event"
        }
    }

    fun createANumberOfProblematicEvents(numberOfEvents: Int): List<Pair<NokkelFeilrespons, Feilrespons>> {
        return (1..numberOfEvents).map {
            NokkelFeilrespons("systembruker", it.toString(),"type") to Feilrespons()
        }
    }
}
