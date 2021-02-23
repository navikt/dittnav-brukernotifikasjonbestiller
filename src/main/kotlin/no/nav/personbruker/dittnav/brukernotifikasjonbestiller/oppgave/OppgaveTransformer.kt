package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validateDateTime

object OppgaveTransformer {

    fun toOppgaveInternal(externalOppgave: Oppgave): OppgaveIntern {
        return OppgaveIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(validateDateTime(externalOppgave.getTidspunkt(), "tidspunkt"))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalOppgave.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setTekst(validateNonNullFieldMaxLength(externalOppgave.getTekst(), "tekst", MAX_LENGTH_TEXT_BESKJED))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalOppgave.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.OPPGAVE)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalOppgave.getSikkerhetsnivaa()))
                .setEksternVarsling(externalOppgave.getEksternVarsling())
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalOppgave: Oppgave): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalOppgave.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .build()
    }
}
