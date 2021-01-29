package no.nav.personbruker.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import org.apache.kafka.clients.consumer.ConsumerRecords

class DoneEventService() : EventBatchProcessorService<Nokkel, Done> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        TODO("Not yet implemented")
    }

}
