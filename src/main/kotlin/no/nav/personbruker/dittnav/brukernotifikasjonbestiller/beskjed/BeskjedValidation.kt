package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import org.apache.avro.generic.GenericRecord
import java.net.MalformedURLException
import java.net.URL

class BeskjedValidation(beskjedInput: BeskjedInput) {
    val failedValidators: List<BeskjedValidator>

    init {
        failedValidators = getFailedValidators(beskjedInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(beskjedInput: BeskjedInput) = listOf(
        TekstIsMax300Characters(),
        LinkIsURLandMax200Characters(),
        SikkerhetsnivaaIs3Or4(),
        PrefererteKanalerIsSMSorEpost(),
        SmstekstIsMax160Characters(),
        EposttekstIsMax4000Characters(),
        EposttittelIsMax40Characters()
    //synligFremtil? må være frem i tid?
    ).filter{ !it.validate(beskjedInput) }
}

abstract class BeskjedValidator {
    abstract val description: String

    abstract fun validate(beskjedInput: BeskjedInput): Boolean
}

class TekstIsMax300Characters: BeskjedValidator() {
    private val MAX_LENGTH_TEXT_BESKJED = 300
    private val fieldName = "tekst"
    override val description: String = "Tekst kan ikke være null, og maks $MAX_LENGTH_TEXT_BESKJED tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNotNull(fieldName) && (beskjedInput.get(fieldName) as String).length <= MAX_LENGTH_TEXT_BESKJED
}

class LinkIsURLandMax200Characters: BeskjedValidator() {
    private val MAX_LENGTH_LINK = 200
    private val fieldName = "link"
    override val description: String = "Link må være gyldig URL og maks $MAX_LENGTH_LINK tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean {
        return beskjedInput.isNull(fieldName) || isValidURL(beskjedInput.get(fieldName) as String)
    }

    private fun isValidURL(link: String) =
        link.length <= MAX_LENGTH_LINK && try {
            URL(link)
            true
        } catch (e: MalformedURLException) {
            false
        }
}

class SikkerhetsnivaaIs3Or4: BeskjedValidator() {
    private val fieldName = "sikkerhetsnivaa"
    override val description: String = "Sikkerhetsnivaa må være 3 eller 4, default er 4"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNull(fieldName) || (beskjedInput.get(fieldName) as Int) in listOf(3,4)
}

class PrefererteKanalerIsSMSorEpost: BeskjedValidator() {
    private val fieldName = "prefererteKanaler"
    override val description: String = "Preferte kanaler kan bare inneholde SMS og EPOST"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNull(fieldName) || (beskjedInput.get(fieldName) as List<*>).all { it in listOf("SMS", "EPOST") }
}

class SmstekstIsMax160Characters: BeskjedValidator() {
    private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
    private val fieldName = "smsVarslingstekst"
    override val description: String = "Sms-varsel kan ikke være tom string, og maks $MAX_LENGTH_SMS_VARSLINGSTEKST tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNull(fieldName) || (beskjedInput.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_SMS_VARSLINGSTEKST
        }
}

class EposttekstIsMax4000Characters: BeskjedValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
    private val fieldName = "epostVarslingstekst"
    override val description: String = "Epost-tekst kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTEKST tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNull(fieldName) || (beskjedInput.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTEKST
        }
}

class EposttittelIsMax40Characters: BeskjedValidator() {
    private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
    private val fieldName = "epostVarslingstittel"
    override val description: String = "Epost-tittel kan ikke være tom string, og maks $MAX_LENGTH_EPOST_VARSLINGSTTITTEL tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNull(fieldName) || (beskjedInput.get(fieldName) as String).trim().let {
            it.isNotEmpty() && it.length <= MAX_LENGTH_EPOST_VARSLINGSTTITTEL
        }
}

private fun GenericRecord.isNotNull(fieldName: String): Boolean = hasField(fieldName) && get(fieldName) != null
private fun GenericRecord.isNull(fieldName: String): Boolean = !hasField(fieldName) || get(fieldName) == null