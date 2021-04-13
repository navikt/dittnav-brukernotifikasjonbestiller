package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class EventDispatcher(private val eventtype: Eventtype, private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {

    fun <T> sendEventsToInternalTopic(successfullyValidatedEvents: Map<NokkelIntern, T>, kafkaProducer: KafkaProducerWrapper<NokkelIntern, T>) {
        val eventsToSendKafka = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
        kafkaProducer.sendEvents(eventsToSendKafka)
    }

    suspend fun <T> persistToDB(successfullyValidatedEvents: Map<NokkelIntern, T>) {
        brukernotifikasjonbestillingRepository.persistInOneBatch(successfullyValidatedEvents, eventtype)
    }

}