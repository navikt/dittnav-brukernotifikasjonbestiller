package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BeskjedEventService(
        private val internalEventProducer: KafkaProducerWrapper<NokkelIntern, BeskjedIntern>,
        private val feilresponsEventProducer: KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Beskjed> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, BeskjedIntern>>()
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.BESKJED) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalBeskjed = event.value()
                    val internalNokkel = BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
                    val internalBeskjed = BeskjedTransformer.toBeskjedInternal(externalBeskjed)
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalBeskjed))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Beskjed-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), fve, Eventtype.BESKJED)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av beskjed-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (e: Exception) {
                    countFailedEventForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), e, Eventtype.BESKJED)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av beskjed-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            //TODO count duplicate events

            if (successfullyValidatedEvents.isNotEmpty()) {
                internalEventProducer.sendEvents(successfullyValidatedEvents)
            }

            if (problematicEvents.isNotEmpty()) {
                feilresponsEventProducer.sendEvents(problematicEvents)
            }
        }
    }

}
