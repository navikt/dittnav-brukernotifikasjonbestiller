package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.toLocalDateTime
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselValidation
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BeskjedInputEventService(
    private val metricsCollector: MetricsCollector,
    private val beskjedRapidProducer: BeskjedRapidProducer,
    private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository
) : EventBatchProcessorService<NokkelInput, BeskjedInput> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedInputEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, BeskjedInput>) {
        metricsCollector.recordMetrics(eventType = Eventtype.BESKJED) {
            val (validEvents, invalidEvents) = events
                .map { it to VarselValidation(it.key(), it.value()) }
                .partition { (_, validation) -> validation.isValid() }

            val uniqueBeskjeder = uniqueBeskjeder(validEvents.map { it.first.toBeskjed() })

            uniqueBeskjeder.forEach {
                beskjedRapidProducer.produce(it)
                countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            invalidEvents.forEach { (beskjed, validation) ->
                log.info(
                    "Ignorerer beskjed på ugyldig format: ${beskjed.key().getEventId()}. " +
                            "Grunn: ${validation.failedValidators.map { it.description }} "
                )
            }

            brukernotifikasjonbestillingRepository.persist(uniqueBeskjeder)
        }
    }

    private suspend fun uniqueBeskjeder(beskjeder: List<Beskjed>): List<Beskjed> {
        val eventIder = beskjeder.map { it.eventId }
        val dbDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsExcludingDone(eventIder).toSet()

        return beskjeder
            .distinctBy { it.eventId }
            .filter { it.eventId !in dbDuplicates }
    }
}

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