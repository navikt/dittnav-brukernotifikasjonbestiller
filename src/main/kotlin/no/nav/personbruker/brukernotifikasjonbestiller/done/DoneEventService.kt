package no.nav.personbruker.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class DoneEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Done>
) : EventBatchProcessorService<Nokkel, Done> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val eventList = events.asWrapperList()
        eventProducer.sendEvents(eventList)
    }
}
