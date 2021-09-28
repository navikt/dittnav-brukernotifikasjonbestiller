package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

class FeilresponsLegacyTransformer(private val mapper: ServiceUserMapper) {

    private val log: Logger = LoggerFactory.getLogger(FeilresponsLegacyTransformer::class.java)

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

    private fun toNokkelFeilrespons(eventId: String, systembruker: String, eventtype: Eventtype): NokkelFeilrespons {
        val origin = getNamespaceAppnameOrUnknown(systembruker)

        return NokkelFeilrespons.newBuilder()
                .setNamespace(origin.namespace)
                .setAppnavn(origin.appName)
                .setSystembruker(systembruker)
                .setEventId(eventId)
                .setBrukernotifikasjonstype(eventtype.toString())
                .build()
    }

    private fun toFeilrespons(exception: Exception): Feilrespons {
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

    private fun getNamespaceAppnameOrUnknown(systembruker: String): NamespaceAppName {
        return try {
            mapper.getNamespaceAppName(systembruker)
        } catch (e: ServiceUserMappingException) {
            NamespaceAppName("ukjent", "ukjent")
        }
    }
}
