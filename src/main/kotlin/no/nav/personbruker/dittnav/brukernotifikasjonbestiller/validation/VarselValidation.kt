package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.validation

import de.huxhorn.sulky.ulid.ULID
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.avro.generic.GenericRecord
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class VarselValidation(nokkel: NokkelInput?, varsel: GenericRecord) {
    val failedValidators: List<VarselValidator>

    init {
        failedValidators = getFailedNokkelValidators(nokkel) +
                if(varsel.schema.name != "DoneInput") getFailedVarselValidators(varsel) else emptyList()
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedVarselValidators(varsel: GenericRecord) = listOf(
        TekstValidator(),
        LinkValidator(),
        SikkerhetsnivaaValidator(),
        PrefererteKanalerValidator(),
        SmstekstValidator(),
        EposttekstValidator(),
        EposttittelValidator()
    ).filter { !it.validate(varsel) }

    private fun getFailedNokkelValidators(nokkel: NokkelInput?) =
        nokkel?.let { nokkelInput ->
            listOf(
                FodselsnummerValidator(),
                NamespaceValidator(),
                AppnavnValidator(),
                EventIdValidator()
            ).filter { !it.validate(nokkelInput) }
        } ?: listOf(HasNokkel())
}

abstract class VarselValidator {
    abstract val description: String

    abstract fun validate(record: GenericRecord): Boolean
}

class HasNokkel : VarselValidator() {
    override val description: String = "Nokkel kan ikke være null"
    override fun validate(record: GenericRecord): Boolean = false //Brukes annerledes enn de andre validatorene
}

class FodselsnummerValidator : VarselValidator() {
    override val description: String = "Fodselsnummer må være 11 tegn"
    private val fieldName = "fodselsnummer"

    override fun validate(record: GenericRecord): Boolean =
        record.get(fieldName)?.let { (it as String).length == 11 } ?: false
}

class NamespaceValidator : VarselValidator() {
    override val description: String = "Namespace kan ikke være null"
    private val fieldName = "namespace"

    override fun validate(record: GenericRecord): Boolean =
        record.isNotNull(fieldName)
}

class AppnavnValidator : VarselValidator() {
    override val description: String = "Appnavn kan ikke være null"
    private val fieldName = "appnavn"

    override fun validate(record: GenericRecord): Boolean =
        record.isNotNull(fieldName)
}

class EventIdValidator : VarselValidator() {
    override val description: String = "Eventid må være gyldig UUID eller ULID"
    private val fieldName = "eventId"

    override fun validate(record: GenericRecord): Boolean =
        record.get(fieldName)?.let {
            (it as String).isValidUuid() || it.isValidUlid()
        } ?: false

    private fun String.isValidUuid(): Boolean =
        try {
            UUID.fromString(this).toString() == this
        } catch (e: IllegalArgumentException) {
            false
        }

    private fun String.isValidUlid(): Boolean =
        try {
            ULID.parseULID(this)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
}

class TekstValidator : VarselValidator() {
    private val maxTextLengthBeskjed = 300
    private val maxTextLengthOppgaveAndInnboks = 500

    private val fieldName = "tekst"
    override val description: String = "Tekst kan ikke være null, og over makslengde"

    override fun validate(record: GenericRecord): Boolean {
        val maxLength = when (record.schema.name) {
            "BeskjedInput" -> maxTextLengthBeskjed
            else -> maxTextLengthOppgaveAndInnboks
        }
        return record.isNotNull(fieldName) && (record.get(fieldName) as String).length <= maxLength
    }
}

class LinkValidator : VarselValidator() {
    private val MAX_LENGTH_LINK = 200
    private val fieldName = "link"
    override val description: String = "Link må være gyldig URL og maks $MAX_LENGTH_LINK tegn"

    override fun validate(record: GenericRecord): Boolean {
        return record.isNull(fieldName)
                || isEmptyBeskjedLink(record)
                || isValidURL(record.get(fieldName) as String)
    }

    private fun isEmptyBeskjedLink(record: GenericRecord): Boolean {
        return record.schema.name == "BeskjedInput" && (record.get(fieldName) as String) == ""
    }

    private fun isValidURL(link: String) =
        link.length <= MAX_LENGTH_LINK && try {
            URL(link)
            true
        } catch (e: MalformedURLException) {
            false
        }
}

class SikkerhetsnivaaValidator : VarselValidator() {
    private val fieldName = "sikkerhetsnivaa"
    override val description: String = "Sikkerhetsnivaa må være 3 eller 4, default er 4"

    override fun validate(record: GenericRecord): Boolean =
        record.isNull(fieldName) || (record.get(fieldName) as Int) in listOf(3, 4)
}

class PrefererteKanalerValidator : VarselValidator() {
    private val fieldName = "prefererteKanaler"
    override val description: String = "Preferte kanaler kan bare inneholde SMS og EPOST"

    override fun validate(record: GenericRecord): Boolean =
        record.isNull(fieldName) || (record.get(fieldName) as List<*>).all { it in listOf("SMS", "EPOST") }
}

class SmstekstValidator : VarselValidator() {
    private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
    private val fieldName = "smsVarslingstekst"
    override val description: String =
        "Sms-varsel kan ikke være tom string, og maks $MAX_LENGTH_SMS_VARSLINGSTEKST tegn"

    override fun validate(record: GenericRecord): Boolean =
        record.isNull(fieldName) || (record.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_SMS_VARSLINGSTEKST
        }
}

class EposttekstValidator : VarselValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
    private val fieldName = "epostVarslingstekst"
    override val description: String =
        "Epost-tekst kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTEKST tegn"

    override fun validate(record: GenericRecord): Boolean =
        record.isNull(fieldName) || (record.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTEKST
        }
}

class EposttittelValidator : VarselValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
    private val fieldName = "epostVarslingstittel"
    override val description: String =
        "Epost-tittel kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTTITTEL tegn"

    override fun validate(record: GenericRecord): Boolean =
        record.isNull(fieldName) || (record.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTTITTEL
        }
}

private fun GenericRecord.isNotNull(fieldName: String): Boolean = hasField(fieldName) && get(fieldName) != null
private fun GenericRecord.isNull(fieldName: String): Boolean = !hasField(fieldName) || get(fieldName) == null
