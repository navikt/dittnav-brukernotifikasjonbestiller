package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.legacy.StatusoppdateringLegacy
import java.time.Instant

object AvroStatusoppdateringLegacyObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultStatusGlobal = "UNDER_BEHANDLING"
    private val defaultStatusIntern = "Intern status"
    private val defaultSakstema = "tema123"

    fun createStatusoppdateringLegacy(): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacy(lopenummer: Int): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultTidspunkt, defaultStatusGlobal, "$defaultStatusIntern-$lopenummer", defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithLink(link: String): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, link, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithFodselsnummer(fodselsnummer: String): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(fodselsnummer, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithGrupperingsId(grupperingsid: String): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, grupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithSikkerhetsnivaa(sikkerhetsnivaa: Int): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, sikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithStatusGlobal(statusGlobal: String): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, statusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithStatusIntern(statusIntern: String?): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, statusIntern, defaultSakstema)
    }

    fun createStatusoppdateringLegacyWithSakstema(sakstema: String): StatusoppdateringLegacy {
        return createStatusoppdateringLegacy(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, sakstema)
    }

    private fun createStatusoppdateringLegacy(fodselsnummer: String, sikkerhetsnivaa: Int, link: String, grupperingsid: String, tidspunkt: Long, statusGlobal: String, statusIntern: String?, sakstema: String): StatusoppdateringLegacy {
        return StatusoppdateringLegacy(
                tidspunkt,
                grupperingsid,
                link,
                sikkerhetsnivaa,
                statusGlobal,
                statusIntern,
                sakstema,
                fodselsnummer
        )
    }
}
