package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import de.huxhorn.sulky.ulid.ULID
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import java.util.UUID

class NokkelValidation(nokkelInput: NokkelInput?) {
    val failedValidators: List<NokkelValidator>

    init {
        failedValidators = getFailedValidators(nokkelInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(nokkelInput: NokkelInput?) = listOf(
        HasNokkel(),
        FodselsnummerIs11Characters(),
        HasNamespace(),
        HasAppnavn(),
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

class HasNamespace: NokkelValidator() {
    override val description: String = "Namespace kan ikke være null"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getNamespace() != null
        } ?: false
}


class HasAppnavn: NokkelValidator() {
    override val description: String = "Appnavn kan ikke være null"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getAppnavn() != null
        } ?: false
}

class EventIdIsUUIDorULID: NokkelValidator() {
    override val description: String = "Eventid må være gyldig UUID eller ULIO"

    override fun validate(nokkelInput: NokkelInput?): Boolean =
        nokkelInput?.let { nokkel ->
            nokkel.getEventId()?.let { it.isValidUuid() || it.isValidUlid() } ?: false
        } ?: false

    private fun String.isValidUuid(): Boolean =
        try { UUID.fromString(this).toString() == this } catch (e: IllegalArgumentException) { false }

    private fun String.isValidUlid(): Boolean =
        try {
            ULID.parseULID(this)
            true
        } catch (e: IllegalArgumentException) { false }
}