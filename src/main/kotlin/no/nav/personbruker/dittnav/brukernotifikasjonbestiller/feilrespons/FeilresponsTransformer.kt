package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsTransformer {

    fun createFeilresponsFromNokkel(eventKey: NokkelInput, exception: Exception, eventtype: Eventtype): Pair<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = toNokkelFeilrespons(
            eventKey.getEventId(),
            eventKey.getNamespace(),
            eventKey.getAppnavn(),
            "N/A",
            eventtype
        )
        val feilrespons = toFeilrespons(exception)

        return Pair(nokkelFeilrespons, feilrespons)
    }

    fun <T> createFeilresponsFromDuplicateEvents(eventtype: Eventtype, duplicateEvents: List<Pair<NokkelIntern, T>>): MutableList<Pair<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        duplicateEvents.forEach { (duplicateEventKey, _) ->
            val duplicateEventException = DuplicateEventException("Dette eventet er allerede opprettet. Nokkel-en er et duplikat, derfor forkaster vi eventet.")
            val feilrespons = createFeilrespons(duplicateEventKey, duplicateEventException, eventtype)
            problematicEvents.add(feilrespons)
        }
        return problematicEvents
    }

    private fun createFeilrespons(eventKey: NokkelIntern, exception: Exception, eventtype: Eventtype): Pair<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = toNokkelFeilrespons(
                eventKey.getEventId(),
                eventKey.getNamespace(),
                eventKey.getAppnavn(),
                eventKey.getSystembruker(),
                eventtype
        )
        val feilrespons = toFeilrespons(exception)

        return Pair(nokkelFeilrespons, feilrespons)
    }

    fun toNokkelFeilrespons(eventId: String, namespace: String, appName: String, systembruker: String, eventtype: Eventtype): NokkelFeilrespons {
        return NokkelFeilrespons.newBuilder()
                .setNamespace(namespace)
                .setAppnavn(appName)
                .setSystembruker(systembruker)
                .setEventId(eventId)
                .setBrukernotifikasjonstype(eventtype.toString())
                .build()
    }

    fun toFeilrespons(exception: Exception): Feilrespons {
        return Feilrespons.newBuilder()
                .setTidspunkt(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setBegrunnelse(getFeilresponsBegrunnelse(exception).toString())
                .setFeilmelding(exception.toString())
                .build()
    }

    private fun getFeilresponsBegrunnelse(exception: Exception): FeilresponsBegrunnelse {
        return when (exception) {
            is FieldValidationException -> FeilresponsBegrunnelse.VALIDERINGSFEIL
            is DuplicateEventException -> FeilresponsBegrunnelse.DUPLIKAT
            else -> FeilresponsBegrunnelse.UKJENT
        }
    }
}
