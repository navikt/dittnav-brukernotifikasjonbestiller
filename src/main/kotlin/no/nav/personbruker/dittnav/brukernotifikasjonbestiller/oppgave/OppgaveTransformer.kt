package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern

object OppgaveTransformer {

    fun toOppgaveInternal(externalNokkel: Nokkel, externalOppgave: Oppgave): OppgaveIntern {
        return OppgaveIntern()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalOppgave: Oppgave): NokkelIntern {
        return NokkelIntern()
    }
}
