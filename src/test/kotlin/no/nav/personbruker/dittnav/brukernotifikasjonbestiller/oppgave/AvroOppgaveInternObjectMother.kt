package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroOppgaveInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun giveMeANumberOfInternalOppgaveEvents(numberOfEvents: Int, eventId: String, systembruker: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, OppgaveIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, OppgaveIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-$i", "$eventId-$i", fodselsnummer)
            val oppgaveIntern = createOppgaveInternWithGrupperingsId("$i")
            events.add(Pair(nokkelIntern, oppgaveIntern))
        }
        return events
    }

    fun createOppgaveInternWithGrupperingsId(grupperingsid: String): OppgaveIntern {
        return createOppgaveIntern(defaultUlid, defaultTidspunkt, grupperingsid, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createOppgaveIntern(ulid: String, tidspunkt: Long, grupperingsid: String, tekst: String, link: String, sikkerhetsnivaa: Int, eksternvarsling: Boolean): OppgaveIntern {
        return OppgaveIntern(
                ulid,
                tidspunkt,
                grupperingsid,
                tekst,
                link,
                sikkerhetsnivaa,
                eksternvarsling
        )
    }
}