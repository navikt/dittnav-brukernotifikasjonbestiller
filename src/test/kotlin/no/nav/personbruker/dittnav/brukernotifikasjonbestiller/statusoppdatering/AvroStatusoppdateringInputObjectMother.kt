package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.input.StatusoppdateringInput
import java.time.Instant

object AvroStatusoppdateringInputObjectMother {

    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultStatusGlobal = "UNDER_BEHANDLING"
    private val defaultStatusIntern = "Intern status"
    private val defaultSakstema = "tema123"

    fun createStatusoppdateringInput(): StatusoppdateringInput {
        return createStatusoppdateringInput(defaultSikkerhetsnivaa, defaultLink, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringInputWithLink(link: String): StatusoppdateringInput {
        return createStatusoppdateringInput(defaultSikkerhetsnivaa, link, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringInputWithSikkerhetsnivaa(sikkerhetsnivaa: Int): StatusoppdateringInput {
        return createStatusoppdateringInput(sikkerhetsnivaa, defaultLink, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringInputWithStatusGlobal(statusGlobal: String): StatusoppdateringInput {
        return createStatusoppdateringInput(defaultSikkerhetsnivaa, defaultLink, defaultTidspunkt, statusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringInputWithStatusIntern(statusIntern: String?): StatusoppdateringInput {
        return createStatusoppdateringInput(defaultSikkerhetsnivaa, defaultLink, defaultTidspunkt, defaultStatusGlobal, statusIntern, defaultSakstema)
    }

    fun createStatusoppdateringInputWithSakstema(sakstema: String): StatusoppdateringInput {
        return createStatusoppdateringInput(defaultSikkerhetsnivaa, defaultLink, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, sakstema)
    }

    private fun createStatusoppdateringInput(sikkerhetsnivaa: Int, link: String, tidspunkt: Long, statusGlobal: String, statusIntern: String?, sakstema: String): StatusoppdateringInput {
        return StatusoppdateringInput(
                tidspunkt,
                link,
                sikkerhetsnivaa,
                statusGlobal,
                statusIntern,
                sakstema,
        )
    }
}
