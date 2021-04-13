package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HandleEvents(private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    private val log: Logger = LoggerFactory.getLogger(HandleEvents::class.java)

    suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, eventtype: Eventtype): List<Brukernotifikasjonbestilling> {
        var result = emptyList<Brukernotifikasjonbestilling>()
        val duplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(successfullyValidatedEvents)

        if (duplicateEventIds.isNotEmpty()) {
            result = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(eventtype, duplicateEventIds)
        }
        return result
    }

    fun createFeilresponsEvents(duplicateEvents: List<Brukernotifikasjonbestilling>, eventtype: Eventtype): MutableList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        duplicateEvents.forEach { duplicateEvent ->
            val duplicateEventException = DuplicateEventException("Dette eventet er allerede opprettet. Nokkel-en er et duplikat, derfor forkaster vi eventet.")
            val feilrespons = FeilresponsTransformer.createFeilrespons(duplicateEvent.eventId, duplicateEvent.systembruker, duplicateEventException, eventtype)
            problematicEvents.add(RecordKeyValueWrapper(feilrespons.key, feilrespons.value))
        }
        return problematicEvents
    }

    fun countDuplicateEvents(eventMetricsSession: EventMetricsSession, duplicateEvents: List<Brukernotifikasjonbestilling>) {
        duplicateEvents.forEach { duplicateEvent ->
            log.info("${duplicateEvent.eventtype} med eventId: ${duplicateEvent.eventId} og systembruker: ${duplicateEvent.eventId} er et duplikat. Legger derfor ikke eventet på topic igjen.")
            eventMetricsSession.countDuplicateEventForSystemUser(duplicateEvent.systembruker)
        }
    }

    fun <T> getRemainingValidatedEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, duplicateEvents: List<Brukernotifikasjonbestilling>, eventtype: Eventtype): Map<NokkelIntern, T> {
        return if (duplicateEvents.isEmpty()) {
            successfullyValidatedEvents
        } else {
            getRemainingEvents(successfullyValidatedEvents, duplicateEvents, eventtype)
        }
    }

    private fun <T> getRemainingEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, duplicateEvents: List<Brukernotifikasjonbestilling>, eventtype: Eventtype): Map<NokkelIntern, T> {
        return successfullyValidatedEvents
                .filter { successfullyValidatedEvent ->
                    !duplicateEvents.any { duplicateEvent ->
                        duplicateEvent.eventId == successfullyValidatedEvent.key.getEventId()
                                && duplicateEvent.systembruker == successfullyValidatedEvent.key.getSystembruker()
                                && duplicateEvent.eventtype == eventtype.toString()
                    }
                }
    }
}