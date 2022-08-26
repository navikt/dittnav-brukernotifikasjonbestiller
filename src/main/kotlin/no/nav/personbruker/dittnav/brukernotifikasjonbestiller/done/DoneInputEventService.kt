package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneInputEventService(
    private val metricsCollector: MetricsCollector,
    private val handleDuplicateEvents: HandleDuplicateDoneEvents,
    private val eventDispatcher: EventDispatcher<DoneIntern>,
    private val doneRapidProducer: DoneRapidProducer,
    private val produceToRapid: Boolean = false
) : EventBatchProcessorService<NokkelInput, DoneInput> {

    private val log: Logger = LoggerFactory.getLogger(DoneInputEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, DoneInput>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, DoneIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalDone = event.value()
                    val internalNokkelDone = DoneInputTransformer.toInternal(externalNokkel, externalDone)
                    successfullyValidatedEvents.add(internalNokkelDone)
                    countSuccessfulEventForProducer(event.namespaceAppName)
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("DoneInput-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), fve, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av DoneInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), cce, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på DoneInput-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, produsent: ${event.namespaceAppName}, $cce", cce)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), e, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av DoneInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {

                val duplicateCheckResult = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
                val duplicateEvents = duplicateCheckResult.duplicateEvents
                val remainingValidatedEvents = duplicateCheckResult.validEvents

                if (duplicateEvents.isNotEmpty()) {
                    problematicEvents.addAll(FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.DONE, duplicateEvents))
                    this.countDuplicateEvents(duplicateEvents)
                }

                if (produceToRapid) {
                    val doneEventer = remainingValidatedEvents.map { Done(it.first.getEventId()) }
                    doneRapidProducer.produce(doneEventer)
                }

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