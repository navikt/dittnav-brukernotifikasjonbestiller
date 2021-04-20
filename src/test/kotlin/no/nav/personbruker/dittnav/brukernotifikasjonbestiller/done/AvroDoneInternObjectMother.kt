package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroDoneInternObjectMother {
    private val defaultUlid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun giveMeANumberOfInternalDoneEvents(numberOfEvents: Int, eventId: String, systembruker: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, DoneIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, DoneIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer)
            val doneIntern = createDoneInternWithGrupperingsId("$i")
            events.add(Pair(nokkelIntern, doneIntern))
        }
        return events
    }

    fun createDoneInternWithGrupperingsId(grupperingsid: String): DoneIntern {
        return createDoneIntern(defaultUlid, defaultTidspunkt, grupperingsid)
    }

    fun createDoneIntern(ulid: String, tidspunkt: Long, grupperingsid: String): DoneIntern {
        return DoneIntern(
                ulid,
                tidspunkt,
                grupperingsid
        )
    }
}