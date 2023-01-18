package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.toLocalDateTime
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class VarselForwarder(
    private val metricsCollector: MetricsCollector,
    private val varselRapidProducer: VarselRapidProducer,
    private val brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository
) {
    private val log: Logger = LoggerFactory.getLogger(VarselForwarder::class.java)

    suspend fun processVarsler(events: ConsumerRecords<NokkelInput, GenericRecord>, eventtype: Eventtype) {
        metricsCollector.recordMetrics(eventType = eventtype) {
            val (validEvents, invalidEvents) = events
                .map { it to VarselValidation(it.key(), it.value()) }
                .partition { (_, validation) -> validation.isValid() }

            val uniqueVarsler = uniqueVarsler(validEvents.map { it.first.toVarsel(eventtype) })

            uniqueVarsler.forEach {
                varselRapidProducer.produce(it)
                countSuccessfulRapidEventForProducer(it.namespace, it.appnavn)
            }

            invalidEvents.forEach { (varsel, validation) ->
                log.info(
                    "Ignorerer varsel på ugyldig format: ${varsel.key().getEventId()}. " +
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

private fun ConsumerRecord<NokkelInput, GenericRecord>.toVarsel(type: Eventtype): Varsel {
    return Varsel(
        type = type,
        systembruker = "N/A",
        namespace = key().getNamespace(),
        appnavn = key().getAppnavn(),
        eventId = key().getEventId(),
        eventTidspunkt = (value().get("tidspunkt") as Long).toLocalDateTime(),
        forstBehandlet = LocalDateTime.now(),
        fodselsnummer = key().getFodselsnummer(),
        grupperingsId = key().getGrupperingsId(),
        tekst = value().get("tekst") as String,
        link = value().get("link") as String,
        sikkerhetsnivaa = value().get("sikkerhetsnivaa") as Int,
        synligFremTil = if (value().hasField("synligFremTil")) {
            (value().get("synligFremTil") as Long).toLocalDateTime() 
        } else null,
        aktiv = true,
        eksternVarsling = value().get("eksternVarsling") as Boolean,
        prefererteKanaler = (value().get("prefererteKanaler") as List<*>).map { it as String },
        smsVarslingstekst = value().get("smsVarslingstekst") as String,
        epostVarslingstekst = value().get("epostVarslingstekst") as String,
        epostVarslingstittel = value().get("epostVarslingstittel") as String
    )
}