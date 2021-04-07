package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsObjectMother {

    fun giveMeANumberOfFeilresponsEvents(numberOfEvents: Int, eventId: String, systembruker: String, eventtype: Eventtype): MutableList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        for (i in 0 until numberOfEvents) {
            val tidspunkt = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            val nokkelFeilrespons = NokkelFeilrespons("$systembruker-$i", "$eventId-$i", eventtype.toString())
            val feilrespons = Feilrespons(tidspunkt, "Simulert feil i test - $i.")
            problematicEvents.add(RecordKeyValueWrapper(nokkelFeilrespons, feilrespons))
        }

        return problematicEvents
    }
}