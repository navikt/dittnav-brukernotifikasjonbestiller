package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.toLocalDateTime
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.avro.generic.GenericRecord
import java.time.LocalDateTime

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

data class Varsel(
    val type: Eventtype,
    val systembruker: String,
    val namespace: String,
    val appnavn: String,
    val eventId: String,
    val eventTidspunkt: LocalDateTime,
    val forstBehandlet: LocalDateTime,
    val fodselsnummer: String,
    val grupperingsId: String,
    val tekst: String,
    val link: String,
    val sikkerhetsnivaa: Int,
    val synligFremTil: LocalDateTime?,
    val aktiv: Boolean,
    val eksternVarsling: Boolean,
    val prefererteKanaler: List<String>,
    val smsVarslingstekst: String?,
    val epostVarslingstekst: String?,
    val epostVarslingstittel: String?
)

private fun Pair<NokkelInput, GenericRecord>.toVarsel(type: Eventtype): Varsel {
    return Varsel(
        type = type,
        systembruker = "N/A",
        namespace = first.getNamespace(),
        appnavn = first.getAppnavn(),
        eventId = first.getEventId(),
        eventTidspunkt = (second.get("tidspunkt") as Long).toLocalDateTime(),
        forstBehandlet = LocalDateTime.now(),
        fodselsnummer = first.getFodselsnummer(),
        grupperingsId = first.getGrupperingsId(),
        tekst = second.get("tekst") as String,
        link = second.get("link") as String,
        sikkerhetsnivaa = second.get("sikkerhetsnivaa") as Int,
        synligFremTil = (second.get("synligFremTil") as Long?)?.toLocalDateTime(),
        aktiv = true,
        eksternVarsling = second.get("eksternVarsling") as Boolean,
        prefererteKanaler = (second.get("prefererteKanaler") as List<*>).map { it as String },
        smsVarslingstekst = second.get("smsVarslingstekst") as String?,
        epostVarslingstekst = second.get("epostVarslingstekst") as String?,
        epostVarslingstittel = second.get("epostVarslingstittel") as String?
    )
}