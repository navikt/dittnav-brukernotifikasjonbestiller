package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
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

class DoneEventService(
        private val internalEventProducer: KafkaProducerWrapper<NokkelIntern, DoneIntern>,
        private val feilresponsEventProducer: KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, DoneIntern>>()
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        events.forEach { event ->
            try {
                val externalNokkel = event.getNonNullKey()
                val externalDone = event.value()
                val internalNokkel = DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
                val internalDone = DoneTransformer.toDoneInternal(externalDone)
                successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalDone))
            } catch (nne: NokkelNullException) {
                log.warn("Eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
            } catch (fve: FieldValidationException) {
                val feilrespons = getFeilrespons(event.key(), fve)
                problematicEvents.add(feilrespons)
                log.warn("Validering av done-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
            } catch (e: Exception) {
                val feilrespons = getFeilrespons(event.key(), e)
                problematicEvents.add(feilrespons)
                log.warn("Transformasjon av done-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
            }
        }
        internalEventProducer.sendEvents(successfullyValidatedEvents)
        feilresponsEventProducer.sendEvents(problematicEvents)
    }

    private fun getFeilrespons(externalNokkel: Nokkel, exception: Exception): RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = FeilresponsTransformer.toNokkelFeilrespons(externalNokkel, Eventtype.DONE)
        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        return RecordKeyValueWrapper(nokkelFeilrespons, feilrespons)
    }
}
