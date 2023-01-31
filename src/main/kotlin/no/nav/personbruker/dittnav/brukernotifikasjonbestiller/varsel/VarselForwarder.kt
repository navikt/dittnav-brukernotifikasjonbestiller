package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
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

            val uniqueVarsler = uniqueVarsler(validEvents.map { it.first.toVarsel(eventtype) })

            uniqueVarsler.forEach {
                varselRapidProducer.produce(it)
                countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            invalidEvents.forEach { (varsel, validation) ->
                log.info(
                    "Ignorerer varsel på ugyldig format: ${varsel.first.getEventId()}. " +
                            "Grunn: ${validation.failedValidators.map { it.description }} "
                )
            }

            brukernotifikasjonbestillingRepository.persistVarsler(uniqueVarsler)
        }
    }

    private suspend fun uniqueVarsler(varsler: List<Varsel>): List<Varsel> {
        val eventIder = varsler.map { it.eventId }
        val dbDuplicates = brukernotifikasjonbestillingRepository.fetchExistingEventIdsExcludingDone(eventIder).toSet()

        return varsler
            .distinctBy { it.eventId }
            .filter { it.eventId !in dbDuplicates }
    }
}