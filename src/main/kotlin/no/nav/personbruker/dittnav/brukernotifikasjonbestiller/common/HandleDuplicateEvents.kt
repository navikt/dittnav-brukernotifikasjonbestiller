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

class HandleDuplicateEvents(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>): List<Brukernotifikasjonbestilling> {
        var result = emptyList<Brukernotifikasjonbestilling>()
        val duplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(successfullyValidatedEvents)

        if (duplicateEventIds.isNotEmpty()) {
            result = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(eventtype, duplicateEventIds)
        }
        return result
    }

    fun createFeilresponsEvents(duplicateEvents: List<Brukernotifikasjonbestilling>): MutableList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        duplicateEvents.forEach { duplicateEvent ->
            val duplicateEventException = DuplicateEventException("Dette eventet er allerede opprettet. Nokkel-en er et duplikat, derfor forkaster vi eventet.")
            val feilrespons = FeilresponsTransformer.createFeilrespons(duplicateEvent.eventId, duplicateEvent.systembruker, duplicateEventException, eventtype)
            problematicEvents.add(RecordKeyValueWrapper(feilrespons.key, feilrespons.value))
        }
        return problematicEvents
    }

    fun <T> getValidatedEventsWithoutDuplicates(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, duplicateEvents: List<Brukernotifikasjonbestilling>): Map<NokkelIntern, T> {
        return if (duplicateEvents.isEmpty()) {
            successfullyValidatedEvents
        } else {
            getRemainingEvents(successfullyValidatedEvents, duplicateEvents)
        }
    }

    private fun <T> getRemainingEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, duplicateEvents: List<Brukernotifikasjonbestilling>): Map<NokkelIntern, T> {
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