package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class StatusoppdateringEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Statusoppdatering>
) : EventBatchProcessorService<Nokkel, Statusoppdatering> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Statusoppdatering>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()
        val eventList = events.asWrapperList()
        events.forEach { event ->
            val externalNokkel = event.key()
            val externalStatusoppdatering = event.value()
            val internalNokkel = StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
            val internalStatusoppdatering = StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalStatusoppdatering))
        }
        eventProducer.sendEvents(eventList)
    }
}
