package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import de.huxhorn.sulky.ulid.ULID
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import java.util.UUID

private val MAX_LENGTH_TEXT_BESKJED = 300
private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
private val MAX_LENGTH_LINK = 200

class NokkelValidation(nokkelInput: NokkelInput?) {
    val isValid: Boolean
    val failedValidators: List<NokkelValidator>

    init {
        val validatorResults = validate(nokkelInput)
        isValid = validatorResults.second.isEmpty()
        failedValidators = validatorResults.second
    }

    private fun validate(nokkelInput: NokkelInput?): Pair<List<NokkelValidator>, List<NokkelValidator>> {

        val validators = listOf(
            HasNokkel(),
            HasFodselsnummer(),
            EventIdIsUUIDorULID()
        )

        return validators.partition{ it.validate(nokkelInput) }

        /*
        beskjedInput.apply {
            getTekst()?.let {
                if(it.length > MAX_LENGTH_TEXT_BESKJED) return false
            } ?: return false

            if(getLink() == null) return false
            if(getLink().length > MAX_LENGTH_LINK) return false
            try {
                URL(getLink())
            } catch (e: MalformedURLException) {
                return false
            }

            if(getSikkerhetsnivaa() !in listOf(3, 4)) return false

            if(getPrefererteKanaler().isNotEmpty()) {
                if(!getEksternVarsling()) return false

                getPrefererteKanaler().forEach { preferertKanal ->
                    try {
                        PreferertKanal.valueOf(preferertKanal)
                    } catch(e: IllegalArgumentException) {
                        return false
                    }
                }
            }

            if(getEpostVarslingstekst() != null) {
                if(!getEksternVarsling()) return false

                if(getEpostVarslingstekst() == "") return false
                if(getEpostVarslingstekst().length > MAX_LENGTH_EPOST_VARSLINGSTEKST) return false
            }

            if(getEpostVarslingstittel() != null) {
                if(!getEksternVarsling()) return false

                if(getEpostVarslingstittel() == "") return false
                if(getEpostVarslingstittel().length > MAX_LENGTH_EPOST_VARSLINGSTTITTEL) return false
            }

            if(getSmsVarslingstekst() != null) {
                if(!getEksternVarsling()) return false

                if(getSmsVarslingstekst() == "") return false
                if(getSmsVarslingstekst().length > MAX_LENGTH_SMS_VARSLINGSTEKST) return false
            }
        }

        return true
         */
    }
}

enum class NokkelValidatorType {
    HasNokkel,
    HasFodselsnummer,
    EventIdIsUUIDorULID
}

abstract class NokkelValidator {
    abstract val description: String
    abstract val type: NokkelValidatorType

    abstract fun validate(nokkelInput: NokkelInput?): Boolean
}

private class HasNokkel: NokkelValidator() {
    override val description: String = "Nokkel kan ikke være null"
    override val type: NokkelValidatorType = NokkelValidatorType.HasNokkel

    override fun validate(nokkelInput: NokkelInput?): Boolean = nokkelInput != null
}

private class HasFodselsnummer: NokkelValidator() {
    override val description: String = "Fodselsnummer kan ikke være null"
    override val type: NokkelValidatorType = NokkelValidatorType.HasFodselsnummer

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { it.getFodselsnummer() != null } ?: false
}

private class EventIdIsUUIDorULID: NokkelValidator() {
    override val description: String = "Eventid må være gyldig UUID eller ULIO"
    override val type: NokkelValidatorType = NokkelValidatorType.EventIdIsUUIDorULID

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let {
            if (it.getEventId() == null) return false
            return it.getEventId().isValidUuid() || it.getEventId().isValidUlid()
        } ?: false

    private fun String.isValidUuid(): Boolean =
        try { UUID.fromString(this).toString() == this } catch (e: IllegalArgumentException) { false }

    private fun String.isValidUlid(): Boolean =
        try {
            ULID.parseULID(this)
            true
        } catch (e: IllegalArgumentException) { false }
}