package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.legacy.InnboksLegacy
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InnboksLegacyEventService(
        private val innboksTransformer: InnboksLegacyTransformer,
        private val feilresponsTransformer: FeilresponsLegacyTransformer,
        private val metricsCollector: MetricsCollector,
        private val handleDuplicateEvents: HandleDuplicateEvents,
        private val eventDispatcher: EventDispatcher<InnboksIntern>
) : EventBatchProcessorService<NokkelLegacy, InnboksLegacy> {

    private val log: Logger = LoggerFactory.getLogger(InnboksLegacyEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelLegacy, InnboksLegacy>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, InnboksIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.INNBOKS) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalInnboks = event.value()
                    val internalNokkel = innboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
                    val internalInnboks = innboksTransformer.toInnboksInternal(externalInnboks)
                    successfullyValidatedEvents.add(Pair(internalNokkel, internalInnboks))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Innboks-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, fve, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av innboks-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, cce, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på innboks-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker, $cce", cce)
                } catch (e: Exception) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, e, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av innboks-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {

                val duplicateCheckResult = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
                val duplicateEvents = duplicateCheckResult.duplicateEvents
                val remainingValidatedEvents = duplicateCheckResult.validEvents

                if (duplicateEvents.isNotEmpty()) {
                    problematicEvents.addAll(FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.INNBOKS, duplicateEvents))
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
