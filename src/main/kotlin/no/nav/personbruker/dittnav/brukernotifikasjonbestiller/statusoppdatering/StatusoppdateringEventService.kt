package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class StatusoppdateringEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Statusoppdatering>
) : EventBatchProcessorService<Nokkel, Statusoppdatering> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Statusoppdatering>) {
        val eventList = events.asWrapperList()
        eventProducer.sendEvents(eventList)
    }
}
