package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import org.apache.avro.generic.GenericRecord
import java.net.MalformedURLException
import java.net.URL

private val MAX_LENGTH_TEXT_BESKJED = 300
private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40

class BeskjedValidation(beskjedInput: BeskjedInput) {
    val failedValidators: List<BeskjedValidator>

    init {
        failedValidators = getFailedValidators(beskjedInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(beskjedInput: BeskjedInput) = listOf(
        TekstIsUnder300Characters(),
        LinkIsURLUnder200Characters(),
        SikkerhetsnivaaIs3Or4()
    ).filter{ !it.validate(beskjedInput) }

    /*
    beskjedInput.apply {
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

abstract class BeskjedValidator {
    abstract val description: String

    abstract fun validate(beskjedInput: BeskjedInput): Boolean
}

class TekstIsUnder300Characters: BeskjedValidator() {
    private val MAX_LENGTH_TEXT_BESKJED = 300
    private val fieldName = "tekst"

    override val description: String = "Tekst kan ikke være null, og må være under $MAX_LENGTH_TEXT_BESKJED tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNotNull(fieldName) && (beskjedInput.get(fieldName) as String).length < MAX_LENGTH_TEXT_BESKJED
}

class LinkIsURLUnder200Characters: BeskjedValidator() {
    private val MAX_LENGTH_LINK = 200
    private val fieldName = "link"

    override val description: String = "Link må være under $MAX_LENGTH_LINK tegn"

    override fun validate(beskjedInput: BeskjedInput): Boolean {
        return beskjedInput.isNull(fieldName) || isValidURL(beskjedInput.get(fieldName) as String)
    }

    private fun isValidURL(link: String) =
        link.length < MAX_LENGTH_LINK && try {
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

private fun GenericRecord.isNotNull(fieldName: String): Boolean = hasField(fieldName) && get(fieldName) != null
private fun GenericRecord.isNull(fieldName: String): Boolean = !hasField(fieldName) || get(fieldName) == null