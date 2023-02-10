package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.LocalDateTimeHelper
import java.time.LocalDateTime

data class Done(
    val eventId: String,
    val forstBehandlet: LocalDateTime,
    val fodselsnummer: String,
    @JsonIgnore
    val namespace: String,
    @JsonIgnore
    val appnavn: String
    )

fun NokkelInput.toDone() =
    Done(
        eventId = getEventId(),
        forstBehandlet = LocalDateTimeHelper.nowAtUtc(),
        fodselsnummer = getFodselsnummer(),
        namespace = getNamespace(),
        appnavn = getAppnavn()
        )