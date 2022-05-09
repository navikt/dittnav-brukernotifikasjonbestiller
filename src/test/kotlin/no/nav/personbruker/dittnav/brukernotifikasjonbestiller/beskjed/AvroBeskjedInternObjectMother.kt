package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroBeskjedInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er en beskjed til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultNamespace = "namespace"
    private val defaultSynligFremTil = Instant.now().toEpochMilli()
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultBehandlet = Instant.now().toEpochMilli()
    private val defaultPrefererteKanaler = emptyList<String>()

    fun giveMeANumberOfInternalBeskjedEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, BeskjedIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, BeskjedIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = createNokkelIntern("$eventId-$i", fodselsnummer, "$systembruker-$i")
            val beskjedIntern = createBeskjedIntern()
            events.add(Pair(nokkelIntern, beskjedIntern))
        }
        return events
    }

    fun createNokkelIntern(eventId: String, fnr: String, systembruker: String): NokkelIntern {
        return AvroNokkelInternObjectMother.createNokkelIntern(defaultUlid, eventId, defaultGrupperingsid, fnr, defaultNamespace, "$systembruker-app", systembruker)
    }

    fun createBeskjedIntern(): BeskjedIntern {
        return createBeskjedIntern(defaultTidspunkt, defaultBehandlet, defaultSynligFremTil, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createBeskjedIntern(tidspunkt: Long, behandlet: Long, synligFremTil: Long, tekst: String, link: String, sikkerhetsnivaa: Int, eksternvarsling: Boolean, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, epostVarslingstittel: String? = null, smsVarslingstekst: String? = null): BeskjedIntern {
        return BeskjedIntern(
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
