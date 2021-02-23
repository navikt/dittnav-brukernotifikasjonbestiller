package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID

object OppgaveTransformer {

    fun toOppgaveInternal(externalOppgave: Oppgave): OppgaveIntern {
        return OppgaveIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(externalOppgave.getTidspunkt())
                .setGrupperingsId(externalOppgave.getGrupperingsId())
                .setTekst(externalOppgave.getTekst())
                .setLink(externalOppgave.getLink())
                .setSikkerhetsnivaa(externalOppgave.getSikkerhetsnivaa())
                .setEksternVarsling(externalOppgave.getEksternVarsling())
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalOppgave: Oppgave): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(externalNokkel.getEventId())
                .setSystembruker(externalNokkel.getSystembruker())
                .setFodselsnummer(externalOppgave.getFodselsnummer())
                .build()
    }
}
