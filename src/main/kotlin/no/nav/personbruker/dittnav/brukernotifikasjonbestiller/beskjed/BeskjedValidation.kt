package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import org.apache.avro.generic.GenericRecord

private val MAX_LENGTH_TEXT_BESKJED = 300
private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
private val MAX_LENGTH_LINK = 200

class BeskjedValidation(beskjedInput: BeskjedInput) {
    val failedValidators: List<BeskjedValidator>

    init {
        failedValidators = getFailedValidators(beskjedInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(beskjedInput: BeskjedInput) = listOf(
        TekstIsUnder200Characters(),
    ).filter{ !it.validate(beskjedInput) }

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

abstract class BeskjedValidator {
    abstract val description: String

    abstract fun validate(beskjedInput: BeskjedInput): Boolean
}

class TekstIsUnder200Characters: BeskjedValidator() {
    override val description: String = "Tekst kan ikke være null, og må være under 200 tegn"
    private val fieldName = "tekst"

    override fun validate(beskjedInput: BeskjedInput): Boolean =
        beskjedInput.isNotNull(fieldName) && beskjedInput.get(fieldName).toString().length < 200
}

private fun GenericRecord.isNotNull(fieldName: String): Boolean = hasField(fieldName) && get(fieldName) != null