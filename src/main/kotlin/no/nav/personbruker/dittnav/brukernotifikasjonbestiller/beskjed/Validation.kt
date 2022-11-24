package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import de.huxhorn.sulky.ulid.ULID
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID

class Validation {

    fun validate(event: ConsumerRecord<NokkelInput, BeskjedInput>): Boolean = validate(event.key(), event.value())

    private fun validate(nokkelInput: NokkelInput?, beskjedInput: BeskjedInput): Boolean {
        val MAX_LENGTH_TEXT_BESKJED = 300
        val MAX_LENGTH_SMS_VARSLINGSTEKST = 160
        val MAX_LENGTH_EPOST_VARSLINGSTEKST = 4000
        val MAX_LENGTH_EPOST_VARSLINGSTTITTEL = 40
        val MAX_LENGTH_LINK = 200

        if(nokkelInput == null) return false
        nokkelInput.apply {
            if(getFodselsnummer() == null) return false
            if(getEventId() == null) return false

            if (!getEventId().isValidUuid() && !getEventId().isValidUlid()) return false
        }

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
    }

    private fun String.isValidUuid(): Boolean =
        try { UUID.fromString(this).toString() == this } catch (e: IllegalArgumentException) { false }

    private fun String.isValidUlid(): Boolean =
        try {
            ULID.parseULID(this)
            true
        } catch (e: IllegalArgumentException) { false }

}