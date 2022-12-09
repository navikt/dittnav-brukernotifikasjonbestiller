package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import org.apache.avro.generic.GenericRecord
import java.net.MalformedURLException
import java.net.URL

class VarselValidation(varsel: GenericRecord) {
    val failedValidators: List<VarselValidator>

    init {
        failedValidators = getFailedValidators(varsel)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(varsel: GenericRecord) = listOf(
        TekstValidator(),
        LinkValidator(),
        SikkerhetsnivaaValidator(),
        PrefererteKanalerValidator(),
        SmstekstValidator(),
        EposttekstValidator(),
        EposttittelValidator()
    //synligFremtil? må være frem i tid?
    ).filter{ !it.validate(varsel) }
}

abstract class VarselValidator {
    abstract val description: String

    abstract fun validate(varsel: GenericRecord): Boolean
}

class TekstValidator: VarselValidator() {
    private val maxTextLengthBeskjed = 300
    private val maxTextLengthOppgaveAndInnboks = 500

    private val fieldName = "tekst"
    override val description: String = "Tekst kan ikke være null, og over makslengde"

    override fun validate(varsel: GenericRecord): Boolean {
        val maxLength = when(varsel.schema.name) {
            "BeskjedInput" -> maxTextLengthBeskjed
            else -> maxTextLengthOppgaveAndInnboks
        }
        return varsel.isNotNull(fieldName) && (varsel.get(fieldName) as String).length <= maxLength
    }
}

class LinkValidator: VarselValidator() {
    private val MAX_LENGTH_LINK = 200
    private val fieldName = "link"
    override val description: String = "Link må være gyldig URL og maks $MAX_LENGTH_LINK tegn"

    override fun validate(varsel: GenericRecord): Boolean {
        return varsel.isNull(fieldName) || isValidURL(varsel.get(fieldName) as String)
    }

    private fun isValidURL(link: String) =
        link.length <= MAX_LENGTH_LINK && try {
            URL(link)
            true
        } catch (e: MalformedURLException) {
            false
        }
}

class SikkerhetsnivaaValidator: VarselValidator() {
    private val fieldName = "sikkerhetsnivaa"
    override val description: String = "Sikkerhetsnivaa må være 3 eller 4, default er 4"

    override fun validate(varsel: GenericRecord): Boolean =
        varsel.isNull(fieldName) || (varsel.get(fieldName) as Int) in listOf(3,4)
}

class PrefererteKanalerValidator: VarselValidator() {
    private val fieldName = "prefererteKanaler"
    override val description: String = "Preferte kanaler kan bare inneholde SMS og EPOST"

    override fun validate(varsel: GenericRecord): Boolean =
        varsel.isNull(fieldName) || (varsel.get(fieldName) as List<*>).all { it in listOf("SMS", "EPOST") }
}

class SmstekstValidator: VarselValidator() {
    private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
    private val fieldName = "smsVarslingstekst"
    override val description: String = "Sms-varsel kan ikke være tom string, og maks $MAX_LENGTH_SMS_VARSLINGSTEKST tegn"

    override fun validate(varsel: GenericRecord): Boolean =
        varsel.isNull(fieldName) || (varsel.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_SMS_VARSLINGSTEKST
        }
}

class EposttekstValidator: VarselValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
    private val fieldName = "epostVarslingstekst"
    override val description: String = "Epost-tekst kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTEKST tegn"

    override fun validate(varsel: GenericRecord): Boolean =
        varsel.isNull(fieldName) || (varsel.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTEKST
        }
}

class EposttittelValidator: VarselValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
    private val fieldName = "epostVarslingstittel"
    override val description: String = "Epost-tittel kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTTITTEL tegn"

    override fun validate(varsel: GenericRecord): Boolean =
        varsel.isNull(fieldName) || (varsel.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTTITTEL
        }
}

private fun GenericRecord.isNotNull(fieldName: String): Boolean = hasField(fieldName) && get(fieldName) != null
private fun GenericRecord.isNull(fieldName: String): Boolean = !hasField(fieldName) || get(fieldName) == null