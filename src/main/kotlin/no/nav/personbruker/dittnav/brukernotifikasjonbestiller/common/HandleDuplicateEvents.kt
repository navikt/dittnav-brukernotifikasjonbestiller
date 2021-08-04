package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class HandleDuplicateEvents(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        val result = mutableListOf<Brukernotifikasjonbestilling>()

        val duplicatesFromKafkaBatch = getDuplicatesFromKafkaBatchThatAreAlreadyInDB(successfullyValidatedEvents)
        val duplicatesThatAlreadyAreInDB = getDuplicatesThatMatchEventsInDB(successfullyValidatedEvents)

        result.addAll(duplicatesFromKafkaBatch)
        result.addAll(duplicatesThatAlreadyAreInDB)
        return result.distinct()
    }

    suspend fun <T> getDuplicatesFromKafkaBatchThatAreAlreadyInDB(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        val allDuplicatesInKafkaBatch = getDuplicatesFromBatch(successfullyValidatedEvents)

        val duplicatesFromKafkaBatchThatAlredyAreInDB = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(eventtype, allDuplicatesInKafkaBatch)
        return duplicatesFromKafkaBatchThatAlredyAreInDB
    }

    private suspend fun <T> getDuplicatesThatMatchEventsInDB(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        var result = emptyList<Brukernotifikasjonbestilling>()
        val duplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(successfullyValidatedEvents)

        if (duplicateEventIds.isNotEmpty()) {
            result = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(eventtype, duplicateEventIds)
        }
        return result
    }

    private fun <T> getDuplicatesFromBatch(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        val result = mutableListOf<Brukernotifikasjonbestilling>()

        val duplicatesInBatch = successfullyValidatedEvents
                .groupingBy { listOf(it.first.getEventId(), it.first.getSystembruker(), eventtype) }
                .eachCount()
                .filter { it.value > 1 }

        if (duplicatesInBatch.isNotEmpty()) {
            duplicatesInBatch.forEach { event ->
                result.add(
                        Brukernotifikasjonbestilling(
                                eventId = event.key[0].toString(),
                                systembruker = event.key[1].toString(),
                                eventtype = Eventtype.valueOf(event.key[2].toString()),
                                prosesserttidspunkt =  java.time.LocalDateTime.now())
                )
            }
        }

        return result
    }

    fun <T> getValidatedEventsWithoutDuplicates(successfullyValidatedEvents: MutableList<Pair<NokkelIntern, T>>, duplicateEvents: List<Brukernotifikasjonbestilling>): List<Pair<NokkelIntern, T>> {
        return if (duplicateEvents.isEmpty()) {
            successfullyValidatedEvents.distinct()
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
                }.distinct()
    }
}