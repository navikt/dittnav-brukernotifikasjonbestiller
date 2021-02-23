package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class BeskjedEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Beskjed>,
) : EventBatchProcessorService<Nokkel, Beskjed> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, BeskjedIntern>>()
        val eventList = events.asWrapperList()
        events.forEach { event ->
            val externalNokkel = event.key()
            val externalBeskjed = event.value()
            val internalNokkel = BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
            val internalBeskjed = BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalBeskjed))
        }
        eventProducer.sendEvents(eventList)
    }
}
