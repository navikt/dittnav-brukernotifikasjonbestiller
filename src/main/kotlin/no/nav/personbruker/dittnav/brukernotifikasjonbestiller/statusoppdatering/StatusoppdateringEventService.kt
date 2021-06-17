package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StatusoppdateringEventService(
        private val metricsCollector: MetricsCollector,
        private val handleDuplicateEvents: HandleDuplicateEvents,
        private val eventDispatcher: EventDispatcher<StatusoppdateringIntern>
) : EventBatchProcessorService<Nokkel, Statusoppdatering> {

    private val log: Logger = LoggerFactory.getLogger(StatusoppdateringEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Statusoppdatering>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, StatusoppdateringIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.STATUSOPPDATERING) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalStatusoppdatering = event.value()
                    val internalNokkel = StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
                    val internalStatusoppdatering = StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
                    successfullyValidatedEvents.add(Pair(internalNokkel, internalStatusoppdatering))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Statusoppdatering-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, fve, Eventtype.STATUSOPPDATERING)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av statusoppdatering-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, cce, Eventtype.STATUSOPPDATERING)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på statusoppdatering-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker, $cce", cce)
                } catch (e: Exception) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, e, Eventtype.STATUSOPPDATERING)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av statusoppdatering-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {
                val duplicateEvents = handleDuplicateEvents.getDuplicateEvents(successfullyValidatedEvents)
                if (duplicateEvents.isNotEmpty()) {
                    problematicEvents.addAll(FeilresponsTransformer.createFeilresponsFromDuplicateEvents(duplicateEvents))
                    this.countDuplicateEvents(duplicateEvents)
                }
                val remainingValidatedEvents = handleDuplicateEvents.getValidatedEventsWithoutDuplicates(successfullyValidatedEvents, duplicateEvents)

                if (problematicEvents.isNotEmpty()) {
                    eventDispatcher.dispatchValidAndProblematicEvents(remainingValidatedEvents, problematicEvents)
                } else {
                    eventDispatcher.dispatchValidEventsOnly(remainingValidatedEvents)
                }
            } else if (problematicEvents.isNotEmpty()) {
                eventDispatcher.dispatchProblematicEventsOnly(problematicEvents)
            }
        }
    }

}
