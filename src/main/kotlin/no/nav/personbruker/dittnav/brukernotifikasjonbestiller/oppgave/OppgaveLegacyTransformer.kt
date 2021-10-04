package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler

class OppgaveLegacyTransformer(private val mapper: ServiceUserMapper) {

    fun toOppgaveInternal(externalOppgave: Oppgave): OppgaveIntern {
        return OppgaveIntern.newBuilder()
                .setTidspunkt(externalOppgave.getTidspunkt())
                .setTekst(validateNonNullFieldMaxLength(externalOppgave.getTekst(), "tekst", MAX_LENGTH_TEXT_OPPGAVE))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalOppgave.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.OPPGAVE)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalOppgave.getSikkerhetsnivaa()))
                .setEksternVarsling(externalOppgave.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalOppgave.getEksternVarsling(), externalOppgave.getPrefererteKanaler()))
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalOppgave: Oppgave): NokkelIntern {
        val origin = mapper.getNamespaceAppName(externalNokkel.getSystembruker())

        return NokkelIntern.newBuilder()
                .setUlid(createULID())
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalOppgave.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalOppgave.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .setNamespace(origin.namespace)
                .setAppnavn(origin.appName)
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .build()
    }
}
