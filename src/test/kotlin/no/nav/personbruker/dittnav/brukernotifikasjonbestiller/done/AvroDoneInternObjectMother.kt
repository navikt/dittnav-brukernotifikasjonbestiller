package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import java.time.Instant

object AvroDoneInternObjectMother {
    private val defaultUlid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultBehandlet = Instant.now().toEpochMilli()
    private val defaultNamespace = "namespace"

    fun giveMeANumberOfInternalDoneEvents(numberOfEvents: Int, eventId: String, systembruker: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, DoneIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, DoneIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = createNokkelIntern("$eventId-$i", "$i", fodselsnummer, systembruker)
            val doneIntern = createDoneIntern()
            events.add(Pair(nokkelIntern, doneIntern))
        }
        return events
    }

    fun createNokkelIntern(eventId: String, grupperingsId: String, fodselsnummer: String, systembruker: String): NokkelIntern {
        return NokkelIntern(
            defaultUlid,
            eventId,
            grupperingsId,
            fodselsnummer,
            defaultNamespace,
            "$systembruker-app",
            systembruker
        )
    }

    fun createDoneIntern(): DoneIntern {
        return DoneIntern(
            defaultTidspunkt,
            defaultBehandlet
        )
    }
}
