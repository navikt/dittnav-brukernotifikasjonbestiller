package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.legacy.DoneLegacy
import java.time.Instant

object AvroDoneLegacyObjectMother {
    private val defaultFodselsnr = "1234"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun createDoneLegacy(): DoneLegacy {
        return createDoneLegacy(defaultFodselsnr, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithFodselsnummer(fodselsnummer: String): DoneLegacy {
        return createDoneLegacy(fodselsnummer, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithGrupperingsId(grupperingsid: String): DoneLegacy {
        return createDoneLegacy(defaultFodselsnr, grupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithTidspunkt(tidspunkt: Long): DoneLegacy {
        return createDoneLegacy(defaultFodselsnr, defaultGrupperingsid, tidspunkt)
    }

    private fun createDoneLegacy(fodselsnummer: String, grupperingsid: String, tidspunkt: Long): DoneLegacy {
        return DoneLegacy(
                tidspunkt,
                fodselsnummer,
                grupperingsid
        )
    }
}
