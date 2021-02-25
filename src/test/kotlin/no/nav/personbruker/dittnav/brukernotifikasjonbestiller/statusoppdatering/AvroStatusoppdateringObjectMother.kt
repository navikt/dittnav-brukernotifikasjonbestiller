package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import java.time.Instant

object AvroStatusoppdateringObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultSikkerhetsnivaa = 4
    private val defaultLink = "http://gyldig.url"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()
    private val defaultStatusGlobal = "UNDER_BEHANDLING"
    private val defaultStatusIntern = "Intern status"
    private val defaultSakstema = "tema123"

    fun createStatusoppdatering(): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdatering(lopenummer: Int): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, "$defaultGrupperingsid-$lopenummer", defaultTidspunkt, defaultStatusGlobal, "$defaultStatusIntern-$lopenummer", defaultSakstema)
    }

    fun createStatusoppdateringWithLink(link: String): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, link, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithFodselsnummer(fodselsnummer: String): Statusoppdatering {
        return createStatusoppdatering(fodselsnummer, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithGrupperingsId(grupperingsid: String): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, grupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, sikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithStatusGlobal(statusGlobal: String): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, statusGlobal, defaultStatusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithStatusIntern(statusIntern: String?): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, statusIntern, defaultSakstema)
    }

    fun createStatusoppdateringWithSakstema(sakstema: String): Statusoppdatering {
        return createStatusoppdatering(defaultFodselsnr, defaultSikkerhetsnivaa, defaultLink, defaultGrupperingsid, defaultTidspunkt, defaultStatusGlobal, defaultStatusIntern, sakstema)
    }

    private fun createStatusoppdatering(fodselsnummer: String, sikkerhetsnivaa: Int, link: String, grupperingsid: String, tidspunkt: Long, statusGlobal: String, statusIntern: String?, sakstema: String): Statusoppdatering {
        return Statusoppdatering(
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
