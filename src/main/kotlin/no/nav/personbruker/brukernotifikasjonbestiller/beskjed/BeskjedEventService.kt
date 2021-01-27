package no.nav.personbruker.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import org.apache.kafka.clients.consumer.ConsumerRecords

class BeskjedEventService() : EventBatchProcessorService<Nokkel, Beskjed> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        TODO("Not yet implemented")
    }
}
