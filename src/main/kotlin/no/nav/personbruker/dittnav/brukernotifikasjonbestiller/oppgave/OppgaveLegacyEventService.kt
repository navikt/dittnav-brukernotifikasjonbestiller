package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEventsLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollectorLegacy
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaveLegacyEventService(
    private val oppgaveTransformer: OppgaveLegacyTransformer,
    private val feilresponsTransformer: FeilresponsLegacyTransformer,
    private val metricsCollector: MetricsCollectorLegacy,
    private val handleDuplicateEvents: HandleDuplicateEventsLegacy,
    private val eventDispatcher: EventDispatcher<OppgaveIntern>
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log: Logger = LoggerFactory.getLogger(OppgaveLegacyEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, OppgaveIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE) {
            events.forEach { event ->
                try {
                    val externalNokkel = event.getNonNullKey()
                    val externalOppgave = event.value()
                    val internalNokkel = oppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
                    val internalOppgave = oppgaveTransformer.toOppgaveInternal(externalOppgave)
                    successfullyValidatedEvents.add(Pair(internalNokkel, internalOppgave))
                    countSuccessfulEventForProducer(internalNokkel.getSystembruker())
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForProducer(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, fve, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForProducer(systembruker)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, cce, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på oppgave-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker, $cce", cce)
                } catch(sme: ServiceUserMappingException) {
                    log.warn("Feil ved henting av systembrukermapping for oppgave-legacy.", sme)
                    throw sme
                } catch (e: Exception) {
                    val systembruker = event.systembruker ?: "NoProducerSpecified"
                    countFailedEventForProducer(systembruker)
                    val feilrespons = feilresponsTransformer.createFeilrespons(event.key().getEventId(), systembruker, e, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av oppgave-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {

                val duplicateCheckResult = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
                val duplicateEvents = duplicateCheckResult.duplicateEvents
                val remainingValidatedEvents = duplicateCheckResult.validEvents

                if (duplicateEvents.isNotEmpty()) {
                    problematicEvents.addAll(FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.OPPGAVE, duplicateEvents))
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
