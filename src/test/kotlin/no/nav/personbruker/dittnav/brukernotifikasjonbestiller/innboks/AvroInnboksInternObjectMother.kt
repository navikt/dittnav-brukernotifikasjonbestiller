package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import java.time.Instant

object AvroInnboksInternObjectMother {

    private val defaultUlid = "54321"
    private val defaultTekst = "Dette er en innboks til bruker."
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultNamespace = "namespace"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultBehandlet = Instant.now().toEpochMilli()
    private val defaultEksternVarsling = false
    private val defaultPrefererteKanaler = emptyList<String>()

    fun giveMeANumberOfInternalInnboksEvents(numberOfEvents: Int, systembruker: String, eventId: String, fodselsnummer: String): MutableList<Pair<NokkelIntern, InnboksIntern>> {
        val events = mutableListOf<Pair<NokkelIntern, InnboksIntern>>()

        for (i in 0 until numberOfEvents) {
            val nokkelIntern = createNokkelIntern("$eventId-$i", fodselsnummer, "$systembruker-$i")
            val innboksIntern = createInnboksIntern()
            events.add(Pair(nokkelIntern, innboksIntern))
        }
        return events
    }

    fun createNokkelIntern(eventId: String, fnr: String, systembruker: String): NokkelIntern {
        return AvroNokkelInternObjectMother.createNokkelIntern(defaultUlid, eventId, defaultGrupperingsid, fnr, defaultNamespace, "$systembruker-app", systembruker)
    }

    fun createInnboksIntern(): InnboksIntern {
        return createInnboksIntern(defaultTidspunkt, defaultBehandlet, defaultTekst, defaultLink, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createInnboksIntern(tidspunkt: Long, behandlet: Long, tekst: String, link: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, epostVarslingstittel: String? = null, smsVarslingstekst: String? = null): InnboksIntern {
        return InnboksIntern(
            tidspunkt,
            behandlet,
            tekst,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }
}
