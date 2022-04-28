package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroOppgaveInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er en oppgave til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultNamespace = "namespace"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultBehandlet = Instant.now().toEpochMilli()
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultEksternVarsling = false
    private val defaultPrefererteKanaler = emptyList<String>()

    fun giveMeANumberOfInternalOppgaveEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, OppgaveIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, OppgaveIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = createNokkelIntern("$eventId-$i", fodselsnummer, "$systembruker-$i")
            val oppgaveIntern = createOppgaveIntern()
            events.add(Pair(nokkelIntern, oppgaveIntern))
        }
        return events
    }

    fun createNokkelIntern(eventId: String, fnr: String, systembruker: String): NokkelIntern {
        return AvroNokkelInternObjectMother.createNokkelIntern(defaultUlid, eventId, defaultGrupperingsid, fnr, defaultNamespace, "$systembruker-app", systembruker)
    }

    fun createOppgaveIntern(): OppgaveIntern {
        return createOppgaveIntern(defaultTidspunkt, defaultBehandlet, defaultSynligFremTil, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createOppgaveIntern(tidspunkt: Long, behandlet: Long, synligFremTil: Long, tekst: String, link: String, sikkerhetsnivaa: Int, eksternvarsling: Boolean, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, epostVarslingstittel: String? = null, smsVarslingstekst: String? = null): OppgaveIntern {
        return OppgaveIntern(
            tidspunkt,
            behandlet,
            synligFremTil,
            tekst,
            link,
            sikkerhetsnivaa,
            eksternvarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }
}
