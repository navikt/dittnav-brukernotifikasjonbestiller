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
                val feilrespons = getFeilrespons(FeilresponsTransformer.toNonNullNokkel(), nne)
                problematicEvents.add(feilrespons)
                log.warn("Eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
            } catch (fve: FieldValidationException) {
                val feilrespons = getFeilrespons(event.key(), fve)
                problematicEvents.add(feilrespons)
                log.warn("Validering av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
            } catch (e: Exception) {
                //TODO: hva gjør vi hvis vi ikke kan konvertere til feilrespons?
                val feilrespons = getFeilrespons(event.key(), e)
                problematicEvents.add(feilrespons)
                log.warn("Transformasjon av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
            }
        }
        internalEventProducer.sendEvents(successfullyValidatedEvents)
        feilresponsEventProducer.sendEvents(problematicEvents)
    }

    private fun getFeilrespons(externalNokkel: Nokkel, exception: Exception): RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = FeilresponsTransformer.toNokkelFeilrespons(externalNokkel, Eventtype.OPPGAVE)
        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        return RecordKeyValueWrapper(nokkelFeilrespons, feilrespons)
    }
}
