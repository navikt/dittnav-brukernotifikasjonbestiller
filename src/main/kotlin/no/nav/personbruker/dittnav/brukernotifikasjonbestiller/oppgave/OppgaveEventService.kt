package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
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

class OppgaveEventService(
        private val metricsCollector: MetricsCollector,
        private val handleDuplicateEvents: HandleDuplicateEvents,
        private val eventDispatcher: EventDispatcher<OppgaveIntern>
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log: Logger = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, OppgaveIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalOppgave = event.value()
                    val internalNokkel = OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
                    val internalOppgave = OppgaveTransformer.toOppgaveInternal(externalOppgave)
                    successfullyValidatedEvents.add(Pair(internalNokkel, internalOppgave))
                    countSuccessfulEventForSystemUser(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, fve, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, cce, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på oppgave-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker, $cce", cce)
                } catch (e: Exception) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForSystemUser(systembruker)
                    val feilrespons = FeilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, e, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
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
