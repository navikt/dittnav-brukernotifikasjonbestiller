package no.nav.personbruker.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class StatusoppdateringEventService(
        private val eventProducer: KafkaProducerWrapper<Statusoppdatering>
) : EventBatchProcessorService<Nokkel, Statusoppdatering> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Statusoppdatering>) {
        TODO("Not yet implemented")
    }
}
