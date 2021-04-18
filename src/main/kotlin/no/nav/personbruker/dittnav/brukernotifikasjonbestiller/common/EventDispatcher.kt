package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class EventDispatcher<T>(
        private val eventtype: Eventtype,
        private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository,
        private val internalEventProducer: KafkaProducerWrapper<NokkelIntern, T>,
        private val feilresponsEventProducer: KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>
) {

    suspend fun dispatchSuccessfullyValidatedEvents(successfullyValidatedEvents: List<Pair<NokkelIntern, T>>) {
        val eventsToSendKafka = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.first, it.second) }
        internalEventProducer.sendEvents(eventsToSendKafka)
        brukernotifikasjonbestillingRepository.persistInOneBatch(successfullyValidatedEvents, eventtype)
    }

    fun dispatchProblematicEvents(problematicEvents: List<Pair<NokkelFeilrespons, Feilrespons>>) {
        val eventsToSendKafka = problematicEvents.map { RecordKeyValueWrapper(it.first, it.second) }
        feilresponsEventProducer.sendEvents(eventsToSendKafka)
    }
}