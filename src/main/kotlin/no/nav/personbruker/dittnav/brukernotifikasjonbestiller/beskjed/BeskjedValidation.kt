package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput

private val MAX_LENGTH_TEXT_BESKJED = 300
private val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
private val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
private val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
private val MAX_LENGTH_LINK = 200

class BeskjedValidation(beskjedInput: BeskjedInput?) {
    val failedValidators: List<BeskjedValidator>

    init {
        failedValidators = getFailedValidators(beskjedInput)
    }

    fun isValid(): Boolean = failedValidators.isEmpty()

    private fun getFailedValidators(beskjedInput: BeskjedInput?) = listOf(
        HasTekst(),
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

    abstract fun validate(beskjedInput: BeskjedInput?): Boolean
}

class HasTekst: BeskjedValidator() {
    override val description: String = "Nokkel kan ikke være null"

    override fun validate(beskjedInput: BeskjedInput?): Boolean = beskjedInput != null
}