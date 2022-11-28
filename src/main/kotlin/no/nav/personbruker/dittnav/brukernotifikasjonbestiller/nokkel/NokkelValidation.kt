package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import de.huxhorn.sulky.ulid.ULID
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import java.util.UUID

private val MAX_LENGTH_TEXT_BESKJED = 300
private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
private val MAX_LENGTH_LINK = 200

class NokkelValidation(nokkelInput: NokkelInput?) {
    val failedValidators: List<NokkelValidator>

    init {
        failedValidators = getFailedValidators(nokkelInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(nokkelInput: NokkelInput?) = listOf(
        HasNokkel(),
        FodselsnummerIs11Characters(),
        NamespaceIsUnder64Characters(),
        AppnavnIsUnder100Characters(),
        EventIdIsUUIDorULID()
    ).filter{ !it.validate(nokkelInput) }
}

abstract class NokkelValidator {
    abstract val description: String

    abstract fun validate(nokkelInput: NokkelInput?): Boolean
}

class HasNokkel: NokkelValidator() {
    override val description: String = "Nokkel kan ikke være null"

    override fun validate(nokkelInput: NokkelInput?): Boolean = nokkelInput != null
}

class FodselsnummerIs11Characters: NokkelValidator() {
    override val description: String = "Fodselsnummer må være 11 tegn"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getFodselsnummer()?.let { it.length == 11 } ?: false
        } ?: false
}

class NamespaceIsUnder64Characters: NokkelValidator() {
    override val description: String = "Namespace kan ikke være null, og må være mindre enn 64 tegn"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getNamespace()?.let { it.length < 64 } ?: false
        } ?: false
}


class AppnavnIsUnder100Characters: NokkelValidator() {
    override val description: String = "Appnavn kan ikke være null"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getAppnavn()?.let { it.length < 100 } ?: false
        } ?: false
}

class EventIdIsUUIDorULID: NokkelValidator() {
    override val description: String = "Eventid må være gyldig UUID eller ULIO"

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