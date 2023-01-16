package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
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
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BeskjedInputEventService(
    private val metricsCollector: MetricsCollector,
    private val handleDuplicateEvents: HandleDuplicateEvents,
    private val eventDispatcher: EventDispatcher<BeskjedIntern>,
    private val beskjedRapidProducer: BeskjedRapidProducer,
    private val produceToRapid: Boolean = false
) : EventBatchProcessorService<NokkelInput, BeskjedInput> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedInputEventService::class.java)

    suspend fun processEvents2(events: ConsumerRecords<NokkelInput, BeskjedInput>) {

        metricsCollector.recordMetrics(eventType = Eventtype.BESKJED) {
            val (validEvents, invalidEvents) = events
                .map { it to VarselValidation(it.key(), it.value()) }
                .partition { (_, validation) -> validation.isValid() }

            val validerteBeskjeder = validEvents.map { it.first.toBeskjed() }
            val unikeBeskjeder = handleDuplicateEvents.unikeBeskjeder(validerteBeskjeder)

            unikeBeskjeder.forEach {
                try { // Trenger vi fortsatt try catch??
                    beskjedRapidProducer.produce(it)
                    countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
                } catch (e: Exception) {
                    log.error("Klarte ikke produsere beskjed ${it.eventId} på rapid", e)
                }
            }

            //eventDispatcher.dispatchProblematicEventsOnly(problematicEvents)
            //brukernotifikasjonbestillingRepository.persistInOneBatch(validatedEvents, eventtype)
        }

        //duplikatsjekke?? holder å ignorere
        //produsere til rapid
        //Lagre
        //produsere til feiltopic
        //logge
    }

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, BeskjedInput>) {
        val successfullyValidatedEvents = mutableListOf<Pair<NokkelIntern, BeskjedIntern>>()
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        metricsCollector.recordMetrics(eventType = Eventtype.BESKJED) {
            events.forEach { event ->
                try {
                    val nokkelExternal = event.getNonNullKey()
                    val beskjedExternal = event.value()
                    val internalNokkelBeskjed = BeskjedInputTransformer.toInternal(nokkelExternal, beskjedExternal)
                    successfullyValidatedEvents.add(internalNokkelBeskjed)
                    countSuccessfulEventForProducer(event.namespaceAppName)
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("BeskjedInput-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), fve, Eventtype.BESKJED)
                    problematicEvents.add(feilrespons)
                    log.warn("Validering av BeskjedInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", fve)
                } catch (cce: ClassCastException) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val funnetType = event.javaClass.name
                    val eventId = event.key().getEventId()
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), cce, Eventtype.BESKJED)
                    problematicEvents.add(feilrespons)
                    log.warn("Feil eventtype funnet på BeskjedInput-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, produsent: ${event.namespaceAppName}, $cce", cce)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.namespaceAppName)
                    val feilrespons = FeilresponsTransformer.createFeilresponsFromNokkel(event.key(), e, Eventtype.BESKJED)
                    problematicEvents.add(feilrespons)
                    log.warn("Transformasjon av BeskjedInput-event fra Kafka feilet, fullfører batch-en før vi skriver til feilrespons-topic.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {

                val duplicateCheckResult = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
                val duplicateEvents = duplicateCheckResult.duplicateEvents
                val remainingValidatedEvents = duplicateCheckResult.validEvents

                if (duplicateEvents.isNotEmpty()) {
                    problematicEvents.addAll(FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.BESKJED, duplicateEvents))
                    this.countDuplicateEvents(duplicateEvents)
                }

                if (produceToRapid) {
                    remainingValidatedEvents.forEach {
                        try {
                            beskjedRapidProducer.produce(it.toBeskjed())
                            countSuccessfulRapidEventForProducer(
                                NamespaceAppName(
                                    namespace = it.first.getNamespace(),
                                    appName = it.first.getAppnavn()
                                )
                            )
                        } catch (e: Exception) {
                            log.error("Klarte ikke produsere beskjed ${it.first.getEventId()} på rapid", e)
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

private fun Pair<NokkelIntern, BeskjedIntern>.toBeskjed() =
    Beskjed(
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

private fun ConsumerRecord<NokkelInput, BeskjedInput>.toBeskjed() =
    Beskjed(
        systembruker = "N/A",
        namespace = key().getNamespace(),
        appnavn = key().getAppnavn(),
        eventId = key().getEventId(),
        eventTidspunkt = value().getTidspunkt().toLocalDateTime(),
        forstBehandlet = LocalDateTime.now(),
        fodselsnummer = key().getFodselsnummer(),
        grupperingsId = key().getGrupperingsId(),
        tekst = value().getTekst(),
        link = value().getLink(),
        sikkerhetsnivaa = value().getSikkerhetsnivaa(),
        synligFremTil = if (value().getSynligFremTil() != null) value().getSynligFremTil().toLocalDateTime() else null,
        aktiv = true,
        eksternVarsling = value().getEksternVarsling(),
        prefererteKanaler = value().getPrefererteKanaler(),
        smsVarslingstekst = value().getSmsVarslingstekst(),
        epostVarslingstekst = value().getEpostVarslingstekst(),
        epostVarslingstittel = value().getEpostVarslingstittel()
    )