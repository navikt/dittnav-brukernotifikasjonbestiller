package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class HandleDuplicateEventsLegacy(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

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

        val possibleDuplicates = brukernotifikasjonbestillingRepository.fetchBrukernotifikasjonKeysThatMatchEventIds(eventIds).toSet()

        return events.partition {
            possibleDuplicates.doesNotContain(it.toBrukernotifikasjonKey(eventtype))
        }.let {
            DuplicateCheckResult(validEvents = it.first, duplicateEvents = it.second)
        }
    }

    private fun <T> getDuplicatesWithinBatch(eventtype: Eventtype, events: List<Pair<NokkelIntern, T>>): DuplicateCheckResult<T> {
        val validEvents = mutableListOf<Pair<NokkelIntern, T>>()
        val validEventKeys = mutableSetOf<BrukernotifikasjonKey>()
        val duplicateEvents = mutableListOf<Pair<NokkelIntern, T>>()

        events.forEach { event ->
            val key = event.toBrukernotifikasjonKey(eventtype)

            if (validEventKeys.doesNotContain(key)) {
                validEvents.add(event)
                validEventKeys.add(key)
            } else {
                duplicateEvents.add(event)
            }
        }

        return DuplicateCheckResult(validEvents, duplicateEvents)
    }

    private fun <T> Pair<NokkelIntern, T>.toBrukernotifikasjonKey(eventtype: Eventtype): BrukernotifikasjonKey {
        return BrukernotifikasjonKey(
                first.getEventId(),
                first.getSystembruker(),
                eventtype
        )
    }

    private fun <T> Set<T>.doesNotContain(entry: T) = !contains(entry)
}
