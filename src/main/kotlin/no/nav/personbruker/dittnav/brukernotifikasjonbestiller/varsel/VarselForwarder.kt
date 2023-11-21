package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.validation.VarselValidation
import org.apache.avro.generic.GenericRecord

class VarselForwarder(
    private val metricsCollector: MetricsCollector,
    private val varselRapidProducer: VarselRapidProducer,
    private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository
) {
    private val log = KotlinLogging.logger {}

    suspend fun processVarsler(events: List<Pair<NokkelInput, GenericRecord>>, eventtype: Eventtype) {
        metricsCollector.recordMetrics(eventType = eventtype) {
            val (validEvents, invalidEvents) = events
                .map { it to VarselValidation(it.first, it.second) }
                .partition { (_, validation) -> validation.isValid() }

            val (uniqueVarsler, duplicateVarsler) = validEvents.map { it.first.toVarsel(eventtype) }.partitionUnique()

            uniqueVarsler.forEach {
                varselRapidProducer.produce(it)
                countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            countDuplicateVarsler(duplicateVarsler)

            invalidEvents.forEach { (varsel, validation) ->
                log.info {
                    "Ignorerer varsel på ugyldig format: ${varsel.first.getEventId()}. " +
                        "Grunn: ${validation.failedValidators.map { it.description }} "
                }
            }

            brukernotifikasjonbestillingRepository.persistVarsler(uniqueVarsler)
        }
    }

    private suspend fun List<Varsel>.partitionUnique(): Pair<List<Varsel>, List<Varsel>> {
        val eventIder = this.map { it.eventId }
        val dbDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsExcludingDone(eventIder).toSet()

        val uniqueVarsler = this
            .distinctBy { it.eventId }
            .filter { it.eventId !in dbDuplicates }

        val duplicateVarsler = this - uniqueVarsler.toSet()

        return Pair(uniqueVarsler, duplicateVarsler)
    }
}
