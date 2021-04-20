package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class HandleDuplicateEvents(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        var result = emptyList<Brukernotifikasjonbestilling>()
        val duplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(successfullyValidatedEvents)

        if (duplicateEventIds.isNotEmpty()) {
            result = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(eventtype, duplicateEventIds)
        }
        return result
    }

    fun <T> getValidatedEventsWithoutDuplicates(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>, duplicateEvents: List<Brukernotifikasjonbestilling>): List<Pair<NokkelIntern, T>> {
        return if (duplicateEvents.isEmpty()) {
            successfullyValidatedEvents
        } else {
            getRemainingEvents(successfullyValidatedEvents, duplicateEvents)
        }
    }

    private fun <T> getRemainingEvents(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>, duplicateEvents: List<Brukernotifikasjonbestilling>): List<Pair<NokkelIntern, T>> {
        return successfullyValidatedEvents
                .filter { successfullyValidatedEvent ->
                    !duplicateEvents.any { duplicateEvent ->
                        duplicateEvent.eventId == successfullyValidatedEvent.first.getEventId()
                                && duplicateEvent.systembruker == successfullyValidatedEvent.first.getSystembruker()
                                && duplicateEvent.eventtype == eventtype
                    }
                }
    }
}