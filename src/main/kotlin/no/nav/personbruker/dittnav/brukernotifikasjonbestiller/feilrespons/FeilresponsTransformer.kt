package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsTransformer {

    private val log: Logger = LoggerFactory.getLogger(FeilresponsTransformer::class.java)

    fun createFeilresponsFromDuplicateEvents(duplicateEvents: List<Brukernotifikasjonbestilling>): MutableList<Pair<NokkelFeilrespons, Feilrespons>> {
        val problematicEvents = mutableListOf<Pair<NokkelFeilrespons, Feilrespons>>()

        duplicateEvents.forEach { duplicateEvent ->
            val duplicateEventException = DuplicateEventException("Dette eventet er allerede opprettet. Nokkel-en er et duplikat, derfor forkaster vi eventet.")
            val feilrespons = createFeilrespons(duplicateEvent.eventId, duplicateEvent.systembruker, duplicateEventException, duplicateEvent.eventtype)
            problematicEvents.add(feilrespons)
        }
        return problematicEvents
    }

    fun createFeilrespons(eventId: String, systembruker: String, exception: Exception, eventtype: Eventtype): Pair<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = toNokkelFeilrespons(eventId, systembruker, eventtype)
        val feilrespons = toFeilrespons(exception)

        //TODO FJERN DENNE
        log.warn("Feilrespons: eventid: ${nokkelFeilrespons.getEventId()}, " +
                "systembruker: ${nokkelFeilrespons.getSystembruker()}, " +
                "eventtype: ${nokkelFeilrespons.getBrukernotifikasjonstype()}, " +
                "begrunnelse: ${feilrespons.getBegrunnelse()}, " +
                "feilmelding: ${feilrespons.getFeilmelding()}")

        return Pair(nokkelFeilrespons, feilrespons)
    }

    fun toNokkelFeilrespons(eventId: String, systembruker: String, eventtype: Eventtype): NokkelFeilrespons {
        return NokkelFeilrespons.newBuilder()
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
