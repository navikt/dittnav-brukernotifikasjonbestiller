package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselValidation
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneInputEventService(
    private val metricsCollector: MetricsCollector,
    private val doneRapidProducer: DoneRapidProducer,
    private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository,
) : EventBatchProcessorService<NokkelInput, DoneInput> {

    private val log: Logger = LoggerFactory.getLogger(DoneInputEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, DoneInput>) {
        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            val (validEvents, invalidEvents) = events
                .map { it to VarselValidation(it.key(), it.value()) }
                .partition { (_, validation) -> validation.isValid() }

            val uniqueDoneList = unique(validEvents.map { it.first.key().toDone() })

            uniqueDoneList.forEach {
                doneRapidProducer.produce(it)
                //countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            invalidEvents.forEach { (done, validation) ->
                log.info(
                    "Ignorerer done på ugyldig format: ${done.key().getEventId()}. " +
                            "Grunn: ${validation.failedValidators.map { it.description }} "
                )
            }

            brukernotifikasjonbestillingRepository.persistDone(uniqueDoneList)
        }
    }

    private suspend fun unique(doneList: List<Done>): List<Done> {
        val eventIder = doneList.map { it.eventId }
        val dbDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsForDone(eventIder).toSet()

        return doneList
            .distinctBy { it.eventId }
            .filter { it.eventId !in dbDuplicates }
    }
}