package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.*
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import observability.traceVarsel
import org.apache.avro.generic.GenericRecord
import org.slf4j.MDC
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class VarselActionForwarder(
    private val varselActionProducer: VarselActionProducer
) {
    private val log = KotlinLogging.logger {}
    private val secureLog = KotlinLogging.logger("secureLog")

    fun forwardVarsel(nokkel: NokkelInput, body: GenericRecord) = traceVarsel(id = nokkel.eventId) {
        try {
            eventProcessed(nokkel, body)
            translateVarselAction(nokkel, body).let {
                varselActionProducer.produce(it)
                eventForwarded(nokkel, body)
            }
        } catch (e: VarselValidationException) {
            eventScrapped(nokkel, body)
            log.info { "Forkaster ugyldig varsel" }
            secureLog.info(e) { "Forkaster ugyldig varsel. Årsak: ${e.explanation}" }
        }
    }

    private fun translateVarselAction(nokkel: NokkelInput, body: GenericRecord) = when(body) {
        is BeskjedInput -> {
            MDC.put("action", "opprett")
            MDC.put("type", "beskjed")
            translateBeskjed(nokkel, body)
        }
        is OppgaveInput -> {
            MDC.put("action", "opprett")
            MDC.put("type", "oppgave")
            translateOppgave(nokkel, body)
        }
        is InnboksInput -> {
            MDC.put("action", "opprett")
            MDC.put("type", "innboks")
            translateInnboks(nokkel, body)
        }
        is DoneInput -> {
            MDC.put("action", "inaktiver")
            MDC.put("type", "done")
            translateDone(nokkel)
        }
        else -> throw IllegalArgumentException("Kan ikke oversette ${body::class.simpleName} til varsel")
    }

    private fun translateBeskjed(nokkelInput: NokkelInput, beskjedInput: BeskjedInput) = OutputEvent(
        action = "opprett beskjed",
        varselId = nokkelInput.eventId,
        content = VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            varselId = nokkelInput.eventId
            sensitivitet = beskjedInput.sikkerhetsnivaa.toSensitivitet()
            ident = nokkelInput.fodselsnummer
            tekst = Tekst(
                spraakkode = "nb",
                tekst = beskjedInput.tekst,
                default = true
            )
            link = beskjedInput.link.ifBlank { null }
            aktivFremTil = beskjedInput.synligFremTil?.toUtcZ()
            eksternVarsling = if (beskjedInput.eksternVarsling) {
                EksternVarslingBestilling(
                    prefererteKanaler = beskjedInput.prefererteKanaler.mapPrefererteKanaler(),
                    smsVarslingstekst = beskjedInput.smsVarslingstekst,
                    epostVarslingstittel = beskjedInput.epostVarslingstittel,
                    epostVarslingstekst = beskjedInput.epostVarslingstekst
                )
            } else {
                null
            }
            produsent = Produsent(
                cluster = "ukjent",
                namespace = nokkelInput.namespace,
                appnavn = nokkelInput.appnavn,
            )
        }
    )

    private fun translateOppgave(nokkelInput: NokkelInput, oppgaveInput: OppgaveInput) = OutputEvent(
        action = "opprett oppgave",
        varselId = nokkelInput.eventId,
        content = VarselActionBuilder.opprett {
            type = Varseltype.Oppgave
            varselId = nokkelInput.eventId
            sensitivitet = oppgaveInput.sikkerhetsnivaa.toSensitivitet()
            ident = nokkelInput.fodselsnummer
            tekst = Tekst(
                spraakkode = "nb",
                tekst = oppgaveInput.tekst,
                default = true
            )
            link = oppgaveInput.link
            aktivFremTil = oppgaveInput.synligFremTil?.toUtcZ()
            eksternVarsling = if (oppgaveInput.eksternVarsling) {
                EksternVarslingBestilling(
                    prefererteKanaler = oppgaveInput.prefererteKanaler.mapPrefererteKanaler(),
                    smsVarslingstekst = oppgaveInput.smsVarslingstekst,
                    epostVarslingstittel = oppgaveInput.epostVarslingstittel,
                    epostVarslingstekst = oppgaveInput.epostVarslingstekst
                )
            } else {
                null
            }
            produsent = Produsent(
                cluster = "ukjent",
                namespace = nokkelInput.namespace,
                appnavn = nokkelInput.appnavn,
            )
        }
    )

    private fun translateInnboks(nokkelInput: NokkelInput, innboksInput: InnboksInput) = OutputEvent(
        action = "opprett innboks",
        varselId = nokkelInput.eventId,
        content = VarselActionBuilder.opprett {
            type = Varseltype.Innboks
            varselId = nokkelInput.eventId
            sensitivitet = innboksInput.sikkerhetsnivaa.toSensitivitet()
            ident = nokkelInput.fodselsnummer
            tekst = Tekst(
                spraakkode = "nb",
                tekst = innboksInput.tekst,
                default = true
            )
            link = innboksInput.link
            eksternVarsling = if (innboksInput.eksternVarsling) {
                EksternVarslingBestilling(
                    prefererteKanaler = innboksInput.prefererteKanaler.mapPrefererteKanaler(),
                    smsVarslingstekst = innboksInput.smsVarslingstekst,
                    epostVarslingstittel = innboksInput.epostVarslingstittel,
                    epostVarslingstekst = innboksInput.epostVarslingstekst
                )
            } else {
                null
            }
            produsent = Produsent(
                cluster = "ukjent",
                namespace = nokkelInput.namespace,
                appnavn = nokkelInput.appnavn,
            )
        }
    )

    private fun translateDone(nokkelInput: NokkelInput) = OutputEvent(
        action = "inaktiver varsel",
        varselId = nokkelInput.eventId,
        content = VarselActionBuilder.inaktiver {
            varselId = nokkelInput.eventId
            produsent = Produsent(
                cluster = "ukjent",
                namespace = nokkelInput.namespace,
                appnavn = nokkelInput.appnavn,
            )
        }
    )

    private fun Long.toUtcZ() = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.of("Z"))

    private fun Int.toSensitivitet() = when (this) {
        3 -> Sensitivitet.Substantial
        4 -> Sensitivitet.High
        else -> throw IllegalArgumentException("$this er ikke et gyldig sikkerhetsnivå")
    }

    private fun List<String>.mapPrefererteKanaler() = map {
        when (it.lowercase()) {
            "sms" -> EksternKanal.SMS
            "epost" -> EksternKanal.EPOST
            else -> throw IllegalArgumentException("$it er ikke en gyldig ekstern kanal")
        }
    }

    private fun eventProcessed(nokkel: NokkelInput, record: GenericRecord) {
        VarselForwardingMetrics.registerEventProcessed(record.type, nokkel.producer)
    }
    private fun eventForwarded(nokkel: NokkelInput, record: GenericRecord) {
        VarselForwardingMetrics.registerEventForwarded(record.type, nokkel.producer)
    }
    private fun eventScrapped(nokkel: NokkelInput, record: GenericRecord) {
        VarselForwardingMetrics.registerEventScrapped(record.type, nokkel.producer)
    }

    private val NokkelInput.producer get() = "${namespace}:${appnavn}"

    private val GenericRecord.type get() = when(this) {
        is BeskjedInput -> "beskjed"
        is OppgaveInput -> "oppgave"
        is InnboksInput -> "innboks"
        is DoneInput -> "done"
        else -> throw IllegalArgumentException("${this::class.simpleName} er ikke en gyldig varseltype")
    }
}
