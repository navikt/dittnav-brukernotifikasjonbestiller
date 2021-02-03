package no.nav.personbruker.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class BeskjedEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Beskjed>,
) : EventBatchProcessorService<Nokkel, Beskjed> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val eventList = events.asWrapperList()
        eventProducer.sendEvents(eventList)
    }
}
