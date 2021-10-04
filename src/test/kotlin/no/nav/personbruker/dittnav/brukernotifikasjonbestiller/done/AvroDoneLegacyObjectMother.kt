package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import java.time.Instant

object AvroDoneLegacyObjectMother {
    private val defaultFodselsnr = "1234"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun createDoneLegacy(): Done {
        return createDoneLegacy(defaultFodselsnr, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithFodselsnummer(fodselsnummer: String): Done {
        return createDoneLegacy(fodselsnummer, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithGrupperingsId(grupperingsid: String): Done {
        return createDoneLegacy(defaultFodselsnr, grupperingsid, defaultTidspunkt)
    }

    fun createDoneLegacyWithTidspunkt(tidspunkt: Long): Done {
        return createDoneLegacy(defaultFodselsnr, defaultGrupperingsid, tidspunkt)
    }

    private fun createDoneLegacy(fodselsnummer: String, grupperingsid: String, tidspunkt: Long): Done {
        return Done(
                tidspunkt,
                fodselsnummer,
                grupperingsid
        )
    }
}
