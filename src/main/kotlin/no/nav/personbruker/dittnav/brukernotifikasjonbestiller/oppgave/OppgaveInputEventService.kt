package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.toLocalDateTime
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaveInputEventService(
    private val metricsCollector: MetricsCollector,
    private val handleDuplicateEvents: HandleDuplicateEvents,
    private val eventDispatcher: EventDispatcher<OppgaveIntern>,
    private val oppgaveRapidProducer: OppgaveRapidProducer,
    private val produceToRapid: Boolean = false
) : EventBatchProcessorService<NokkelInput, OppgaveInput> {

    private val log: Logger = LoggerFactory.getLogger(OppgaveInputEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, OppgaveInput>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, OppgaveIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE) {
            events.forEach { event ->
                try {
                    val nokkelExternal = event.getNonNullKey()
                    val oppgaveExternal = event.value()
                    val internalNokkelOppgave = OppgaveInputTransformer.toInternal(nokkelExternal, oppgaveExternal)
                    successfullyValidatedEvents.add(internalNokkelOppgave)
                    countSuccessfulEventForProducer(event.namespaceAppName)
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("OppgaveInput-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), fve, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av OppgaveInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), cce, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på OppgaveInput-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, produsent: ${event.namespaceAppName}, $cce", cce)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), e, Eventtype.OPPGAVE)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av OppgaveInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
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

                if (produceToRapid) {
                    remainingValidatedEvents.forEach {
                        try {
                            oppgaveRapidProducer.produce(it.toOppgave())
                            countSuccessfulRapidEventForProducer(
                                NamespaceAppName(
                                    namespace = it.first.getNamespace(),
                                    appName = it.first.getAppnavn()
                                )
                            )
                        } catch (e: Exception) {
                            log.error("Klarte ikke produsere oppgave ${it.first.getEventId()} på rapid", e)
                        }
                    }
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

private fun Pair<NokkelIntern, OppgaveIntern>.toOppgave() =
    Oppgave(
        systembruker = first.getSystembruker(),
        namespace = first.getNamespace(),
        appnavn = first.getAppnavn(),
        eventId = first.getEventId(),
        eventTidspunkt = second.getTidspunkt().toLocalDateTime(),
        forstBehandlet = second.getBehandlet().toLocalDateTime(),
        fodselsnummer = first.getFodselsnummer(),
        grupperingsId = first.getGrupperingsId(),
        tekst = second.getTekst(),
        link = second.getLink(),
        sikkerhetsnivaa = second.getSikkerhetsnivaa(),
        synligFremTil = if (second.getSynligFremTil() != null) second.getSynligFremTil().toLocalDateTime() else null,
        aktiv = true,
        eksternVarsling = second.getEksternVarsling(),
        prefererteKanaler = second.getPrefererteKanaler(),
        smsVarslingstekst = second.getSmsVarslingstekst(),
        epostVarslingstekst = second.getEpostVarslingstekst(),
        epostVarslingstittel = second.getEpostVarslingstittel()
    )
