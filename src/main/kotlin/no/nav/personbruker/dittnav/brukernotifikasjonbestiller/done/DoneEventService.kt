package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class DoneEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Done>
) : EventBatchProcessorService<Nokkel, Done> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, DoneIntern>>()
        val eventList = events.asWrapperList()
        events.forEach { event ->
            val externalNokkel = event.key()
            val externalDone = event.value()
            val internalNokkel = DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
            val internalDone = DoneTransformer.toDoneInternal(externalNokkel, externalDone)
            successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalDone))
        }
        eventProducer.sendEvents(eventList)
    }
}
