package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
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

class StatusoppdateringEventService(
        private val internalEventProducer: KafkaProducerWrapper<NokkelIntern, StatusoppdateringIntern>,
        private val feilresponsEventProducer: KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Statusoppdatering> {

    private val log: Logger = LoggerFactory.getLogger(StatusoppdateringEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Statusoppdatering>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()
        val problematicEvents = mutableListOf<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.STATUSOPPDATERING) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalStatusoppdatering = event.value()
                    val internalNokkel = StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
                    val internalStatusoppdatering = StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(internalNokkel, internalStatusoppdatering))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Statusoppdatering-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), fve, Eventtype.STATUSOPPDATERING)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av statusoppdatering-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (e: Exception) {
                    countFailedEventForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key(), e, Eventtype.STATUSOPPDATERING)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av statusoppdatering-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
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

}
