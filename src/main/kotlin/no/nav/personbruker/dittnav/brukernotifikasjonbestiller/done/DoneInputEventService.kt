package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.validation.VarselValidation
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.consumer.ConsumerRecords

class DoneInputEventService(
    private val metricsCollector: MetricsCollector,
    private val doneRapidProducer: DoneRapidProducer,
    private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository,
) : EventBatchProcessorService<NokkelInput, DoneInput> {

    private val log = KotlinLogging.logger { }

    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, DoneInput>) {
        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            val (validEvents, invalidEvents) = events
                .filter { it.value() != null }
                .map { it to VarselValidation(it.key(), it.value()) }
                .partition { (_, validation) -> validation.isValid() }

            val (uniqueDoneList, duplicateDoneList) = validEvents.map { it.first.key().toDone() }.partitionUnique()

            uniqueDoneList.forEach {
                doneRapidProducer.produce(it)
                countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            countDuplicateDone(duplicateDoneList)

            invalidEvents.forEach { (done, validation) ->
                log.info {
                    "Ignorerer done på ugyldig format: ${done.key().getEventId()}. " +
                        "Grunn: ${validation.failedValidators.map { it.description }} "
                }
            }

            brukernotifikasjonbestillingRepository.persistDone(uniqueDoneList)
        }
    }

    private suspend fun List<Done>.partitionUnique(): Pair<List<Done>, List<Done>> {
        val eventIder = this.map { it.eventId }
        val dbDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsForDone(eventIder).toSet()

        val uniqueDone = this
            .distinctBy { it.eventId }
            .filter { it.eventId !in dbDuplicates }

        val duplicateDone = this - uniqueDone.toSet()

        return Pair(uniqueDone, duplicateDone)
    }
}
