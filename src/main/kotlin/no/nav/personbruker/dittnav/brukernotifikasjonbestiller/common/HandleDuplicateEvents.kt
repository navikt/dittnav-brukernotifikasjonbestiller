package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository

class HandleDuplicateEvents(private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    suspend fun <T> checkForDuplicateEvents(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val checkDuplicatesInDbResult = getDuplicatesFromDb(successfullyValidatedEvents)
        val checkDuplicatesWithinBatchResult = getDuplicatesWithinBatch(checkDuplicatesInDbResult.validEvents)

        val validEvents = checkDuplicatesWithinBatchResult.validEvents
        val allDuplicates = checkDuplicatesInDbResult.duplicateEvents + checkDuplicatesWithinBatchResult.duplicateEvents

        return DuplicateCheckResult(
                validEvents = validEvents,
                duplicateEvents = allDuplicates
        )
    }

    private suspend fun <T> getDuplicatesFromDb(events: List<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val eventIds = events.map { it.first.getEventId() }

        val possibleDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsExcludingDone(eventIds).toSet()

        return events.partition {
            possibleDuplicates.doesNotContain(it.first.getEventId())
        }.let {
            DuplicateCheckResult(validEvents = it.first, duplicateEvents = it.second)
        }
    }

    private fun <T> getDuplicatesWithinBatch(events: List<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val validEvents = mutableListOf<Pair<NokkelIntern, T>>()
        val validEventIds = mutableSetOf<String>()
        val duplicateEvents = mutableListOf<Pair<NokkelIntern, T>>()

        events.forEach { event ->
            val eventId = event.first.getEventId()

            if (validEventIds.doesNotContain(eventId)) {
                validEvents.add(event)
                validEventIds.add(eventId)
            } else {
                duplicateEvents.add(event)
            }
        }

        return DuplicateCheckResult(validEvents, duplicateEvents)
    }

    private fun <T> Set<T>.doesNotContain(entry: T) = !contains(entry)
}
