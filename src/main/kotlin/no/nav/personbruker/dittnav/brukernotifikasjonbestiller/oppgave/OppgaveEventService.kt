package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class OppgaveEventService(
        private val eventProducer: KafkaProducerWrapper<Nokkel, Oppgave>
) : EventBatchProcessorService<Nokkel, Oppgave> {

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, OppgaveIntern>>()
        val eventList = events.asWrapperList()
        events.forEach { event ->
            val externalNokkel = event.key()
            val externalOppgave = event.value()
            val internalNokkel = OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
            val internalOppgave = OppgaveTransformer.toOppgaveInternal(externalNokkel, externalOppgave)
            successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalOppgave))
        }
        eventProducer.sendEvents(eventList)
    }
}
