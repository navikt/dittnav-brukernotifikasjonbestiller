package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.CurrentTimeHelper.nowInEpochMillis
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelInputTransformer

object DoneInputTransformer {

    fun toInternal(nokkelExternal: NokkelInput, doneExternal: DoneInput): Pair<NokkelIntern, DoneIntern> {

        return NokkelInputTransformer.toNokkelInternalWithoutEventIdValidation(nokkelExternal) to toDoneInternal(doneExternal)
    }

    private fun toDoneInternal(externalDone: DoneInput): DoneIntern {
        return DoneIntern.newBuilder()
            .setTidspunkt(externalDone.getTidspunkt())
            .setBehandlet(nowInEpochMillis())
            .build()
    }
}
