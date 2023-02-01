package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.output.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsObjectMother {

    fun giveMeANumberOfFeilresponsEvents(numberOfEvents: Int, eventId: String, systembruker: String, eventtype: Eventtype): MutableList<Pair<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        for (i in 0 until numberOfEvents) {
            val tidspunkt = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            val nokkelFeilrespons = createNokkelFeilrespons("$eventId-$i", eventtype, "$systembruker-$i")
            val feilrespons = Feilrespons(tidspunkt, FeilresponsBegrunnelse.VALIDERINGSFEIL.toString(), "Simulert feil i test - $i.")
            problematicEvents.add(Pair(nokkelFeilrespons, feilrespons))
        }

        return problematicEvents
    }

    private fun createNokkelFeilrespons(eventId: String, eventtype: Eventtype, systembruker: String) =
            NokkelFeilrespons(
                    eventId,
                    eventtype.name,
                    "namespace",
                    "$systembruker-app",
                    systembruker
            )

    fun createANumberOfProblematicEvents(numberOfEvents: Int): List<Pair<NokkelFeilrespons, Feilrespons>> {
        return (1..numberOfEvents).map {
            NokkelFeilrespons(it.toString(), "type", "namespace", "app", "systembruker") to Feilrespons()
        }
    }
}