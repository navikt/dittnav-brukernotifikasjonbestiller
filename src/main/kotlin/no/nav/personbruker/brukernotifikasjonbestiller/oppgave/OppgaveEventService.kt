package no.nav.personbruker.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import org.apache.kafka.clients.consumer.ConsumerRecords

class OppgaveEventService() : EventBatchProcessorService<Nokkel, Oppgave> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        TODO("Not yet implemented")
    }

}
