package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
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

    suspend fun dispatchSuccessfullyValidatedEvents(successfullyValidatedEvents: List<Pair<NokkelIntern, T>>) {
        val eventsToSendKafka = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.first, it.second) }
        internalEventProducer.sendEventsTransactionally(
                eventsToSendKafka,
                dependentTransaction = { peristValidatedEvents(successfullyValidatedEvents) }
        )
    }

    suspend fun dispatchProblematicEvents(problematicEvents: List<Pair<NokkelFeilrespons, Feilrespons>>) {
        val eventsToSendKafka = problematicEvents.map { RecordKeyValueWrapper(it.first, it.second) }
        feilresponsEventProducer.sendEventsTransactionally(eventsToSendKafka)
    }

    private suspend fun peristValidatedEvents(successfullyValidatedEvents: List<Pair<NokkelIntern, T>>) {
        brukernotifikasjonbestillingRepository.persistInOneBatch(successfullyValidatedEvents, eventtype)
    }
}
