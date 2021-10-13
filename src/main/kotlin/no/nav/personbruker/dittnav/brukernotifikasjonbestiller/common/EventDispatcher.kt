package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class EventDispatcher<T>(
        private val eventtype: Eventtype,
        private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository,
        private val internalEventProducer: Producer<NokkelIntern, T>,
        private val feilresponsEventProducer: Producer<NokkelFeilrespons, Feilrespons>
) {

    suspend fun dispatchValidAndProblematicEvents(
            validatedEvents: List<Pair<NokkelIntern, T>>,
            problematicEvents: List<Pair<NokkelFeilrespons, Feilrespons>>
    ) {
        val validatedEventsToSend = validatedEvents.map { RecordKeyValueWrapper(it.first, it.second) }
        val problematicEventsToSend = problematicEvents.map { RecordKeyValueWrapper(it.first, it.second) }

        try {
            internalEventProducer.sendEventsAndLeaveTransactionOpen(validatedEventsToSend)
            feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(problematicEventsToSend)

            brukernotifikasjonbestillingRepository.persistInOneBatch(validatedEvents, eventtype)

            internalEventProducer.commitCurrentTransaction()
            feilresponsEventProducer.commitCurrentTransaction()
        } catch (e: Exception) {
            internalEventProducer.abortCurrentTransaction()
            feilresponsEventProducer.abortCurrentTransaction()
            throw e
        }
    }

    suspend fun dispatchValidEventsOnly(validatedEvents: List<Pair<NokkelIntern, T>>) {
        val validatedEventsToSend = validatedEvents.map { RecordKeyValueWrapper(it.first, it.second) }

        try {
            internalEventProducer.sendEventsAndLeaveTransactionOpen(validatedEventsToSend)

            brukernotifikasjonbestillingRepository.persistInOneBatch(validatedEvents, eventtype)

            internalEventProducer.commitCurrentTransaction()
        } catch (e: Exception) {
            internalEventProducer.abortCurrentTransaction()
            throw e
        }
    }

    fun dispatchProblematicEventsOnly(problematicEvents: List<Pair<NokkelFeilrespons, Feilrespons>>) {
        val problematicEventsToSend = problematicEvents.map { RecordKeyValueWrapper(it.first, it.second) }

        feilresponsEventProducer.sendEventsTransactionally(problematicEventsToSend)
    }
}
