package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.input.StatusoppdateringInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.CurrentTimeHelper.nowInEpochMillis
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelInputTransformer

object StatusoppdateringInputTransformer {

    fun toInternal(nokkelExternal: NokkelInput, statusoppdateringExternal: StatusoppdateringInput): Pair<NokkelIntern, StatusoppdateringIntern> {

        return NokkelInputTransformer.toNokkelInternal(nokkelExternal) to toStatusoppdateringInternal(statusoppdateringExternal)
    }

    private fun toStatusoppdateringInternal(externalStatusoppdatering: StatusoppdateringInput): StatusoppdateringIntern {
        return StatusoppdateringIntern.newBuilder()
            .setTidspunkt(externalStatusoppdatering.getTidspunkt())
            .setBehandlet(nowInEpochMillis())
            .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalStatusoppdatering.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.STATUSOPPDATERING)))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalStatusoppdatering.getSikkerhetsnivaa()))
            .setStatusGlobal(validateStatusGlobal(externalStatusoppdatering.getStatusGlobal()))
            .setStatusIntern(externalStatusoppdatering.getStatusIntern()?.let { status -> validateMaxLength(status, "statusIntern", MAX_LENGTH_STATUSINTERN)})
            .setSakstema(validateNonNullFieldMaxLength(externalStatusoppdatering.getSakstema(), "sakstema", MAX_LENGTH_SAKSTEMA))
            .build()
    }
}
