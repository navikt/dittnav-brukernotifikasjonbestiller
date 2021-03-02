package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaveEventService(
        private val internalEventProducer: KafkaProducerWrapper<NokkelIntern, OppgaveIntern>,
        private val feilresponsEventProducer: KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log: Logger = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, OppgaveIntern>>()
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        events.forEach { event ->
            try {
                val externalNokkel = event.getNonNullKey()
                val externalOppgave = event.value()
                val internalNokkel = OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
                val internalOppgave = OppgaveTransformer.toOppgaveInternal(externalOppgave)
                successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalOppgave))
            } catch (nne: NokkelNullException) {
                log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
            } catch (fve: FieldValidationException) {
                val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), fve, Eventtype.OPPGAVE)
                problematicEvents.add(feilrespons)
                log.warn("Validering av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
            } catch (e: Exception) {
                val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), e, Eventtype.OPPGAVE)
                problematicEvents.add(feilrespons)
                log.warn("Transformasjon av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
            }
        }

        if (successfullyValidatedEvents.isNotEmpty()) {
            internalEventProducer.sendEvents(successfullyValidatedEvents)
        }

        if (problematicEvents.isNotEmpty()) {
            feilresponsEventProducer.sendEvents(problematicEvents)
        }

    }

}
