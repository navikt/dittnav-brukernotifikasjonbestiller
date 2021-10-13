package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneLegacyEventService(
        private val doneTransformer: DoneLegacyTransformer,
        private val feilresponsTransformer: FeilresponsLegacyTransformer,
        private val metricsCollector: MetricsCollector,
        private val handleDuplicateEvents: HandleDuplicateDoneEvents,
        private val eventDispatcher: EventDispatcher<DoneIntern>
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneLegacyEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, DoneIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalDone = event.value()
                    val internalNokkel = doneTransformer.toNokkelInternal(externalNokkel, externalDone)
                    val internalDone = doneTransformer.toDoneInternal(externalDone)
                    successfullyValidatedEvents.add(Pair(internalNokkel, internalDone))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Done-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, fve, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av done-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, cce, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på done-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker, $cce", cce)
                } catch(sme: ServiceUserMappingException) {
                    log.warn("Feil ved henting av systembrukermapping for done-legacy.", sme)
                    throw sme
                } catch (e: Exception) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, e, Eventtype.DONE)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av done-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
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
