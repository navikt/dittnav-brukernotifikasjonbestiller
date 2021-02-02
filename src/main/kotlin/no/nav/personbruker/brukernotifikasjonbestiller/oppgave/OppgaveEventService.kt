package no.nav.personbruker.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class OppgaveEventService(
        private val eventProducer: KafkaProducerWrapper<Oppgave>
) : EventBatchProcessorService<Nokkel, Oppgave> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        TODO("Not yet implemented")
    }

}
