package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class InnboksInputEventService(
    private val metricsCollector: MetricsCollector,
    private val handleDuplicateEvents: HandleDuplicateEvents,
    private val eventDispatcher: EventDispatcher<InnboksIntern>,
    private val innboksRapidProducer: InnboksRapidProducer,
    private val produceToRapid: Boolean = false
) : EventBatchProcessorService<NokkelInput, InnboksInput> {

    private val log: Logger = LoggerFactory.getLogger(InnboksInputEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, InnboksInput>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, InnboksIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.INNBOKS) {
            events.forEach { event ->
                try {
                    val nokkelExternal = event.getNonNullKey()
                    val innboksExternal = event.value()
                    val internalNokkelInnboks = InnboksInputTransformer.toInternal(nokkelExternal, innboksExternal)
                    successfullyValidatedEvents.add(internalNokkelInnboks)
                    countSuccessfulEventForProducer(event.namespaceAppName)
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("InnboksInput-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), fve, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av InnboksInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), cce, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på InnboksInput-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, produsent: ${event.namespaceAppName}, $cce", cce)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), e, Eventtype.INNBOKS)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av InnboksInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
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

                if (produceToRapid) {
                    val innboksVarsler = remainingValidatedEvents.map { it.toInnboks() }
                    innboksRapidProducer.produce(innboksVarsler)
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

private fun Pair<NokkelIntern, InnboksIntern>.toInnboks() =
    Innboks(
        systembruker = first.getSystembruker(),
        namespace = first.getNamespace(),
        appnavn = first.getAppnavn(),
        eventId = first.getEventId(),
        eventTidspunkt = LocalDateTime.ofInstant(Instant.ofEpochMilli(second.getTidspunkt()), ZoneId.of("UTC")),
        forstBehandlet = LocalDateTime.ofInstant(Instant.ofEpochMilli(second.getBehandlet()), ZoneId.of("UTC")),
        fodselsnummer = first.getFodselsnummer(),
        grupperingsId = first.getGrupperingsId(),
        tekst = second.getTekst(),
        link = second.getLink(),
        sikkerhetsnivaa = second.getSikkerhetsnivaa(),
        aktiv = true,
        eksternVarsling = second.getEksternVarsling(),
        prefererteKanaler = second.getPrefererteKanaler()
    )
