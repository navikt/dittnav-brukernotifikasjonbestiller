package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class HandleDuplicateDoneEventsLegacy(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    suspend fun <T> checkForDuplicateEvents(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val checkDuplicatesInDbResult = getDuplicatesFromDb(eventtype, successfullyValidatedEvents)
        val checkDuplicatesWithinBatchResult = getDuplicatesWithinBatch(eventtype, checkDuplicatesInDbResult.validEvents)

        val validEvents = checkDuplicatesWithinBatchResult.validEvents
        val allDuplicates = checkDuplicatesInDbResult.duplicateEvents + checkDuplicatesWithinBatchResult.duplicateEvents

        return DuplicateCheckResult(
                validEvents = validEvents,
                duplicateEvents = allDuplicates
        )
    }

    private suspend fun <T> getDuplicatesFromDb(eventtype: Eventtype, events: List<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val eventIds = events.map { it.first.getEventId() }

        val possibleDuplicates = brukernotifikasjonbestillingRepository.fetchDoneKeysThatMatchEventIds(eventIds).toSet()

        return events.partition {
            possibleDuplicates.doesNotContain(it.toDoneKey(eventtype))
        }.let {
            DuplicateCheckResult(validEvents = it.first, duplicateEvents = it.second)
        }
    }

    private fun <T> getDuplicatesWithinBatch(eventtype: Eventtype, events: List<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val validEvents = mutableListOf<Pair<NokkelIntern, T>>()
        val validEventKeys = mutableSetOf<DoneKey>()
        val duplicateEvents = mutableListOf<Pair<NokkelIntern, T>>()

        events.forEach { event ->
            val key = event.toDoneKey(eventtype)

            if (validEventKeys.doesNotContain(key)) {
                validEvents.add(event)
                validEventKeys.add(key)
            } else {
                duplicateEvents.add(event)
            }
        }

        return DuplicateCheckResult(validEvents, duplicateEvents)
    }

    private fun <T> Pair<NokkelIntern, T>.toDoneKey(eventtype: Eventtype): DoneKey {
        return DoneKey(
                first.getEventId(),
                first.getSystembruker(),
                eventtype,
                first.getFodselsnummer()
        )
    }

    private fun <T> Set<T>.doesNotContain(entry: T) = !contains(entry)
}
