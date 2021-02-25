package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import java.time.Instant

object AvroDoneObjectMother {
    private val defaultFodselsnr = "1234"
    private val defaultGrupperingsid = "123"
    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun createDone(): Done {
        return createDone(defaultFodselsnr, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneWithFodselsnummer(fodselsnummer: String): Done {
        return createDone(fodselsnummer, defaultGrupperingsid, defaultTidspunkt)
    }

    fun createDoneWithGrupperingsId(grupperingsid: String): Done {
        return createDone(defaultFodselsnr, grupperingsid, defaultTidspunkt)
    }

    fun createDoneWithTidspunkt(tidspunkt: Long): Done {
        return createDone(defaultFodselsnr, defaultGrupperingsid, tidspunkt)
    }

    private fun createDone(fodselsnummer: String, grupperingsid: String, tidspunkt: Long): Done {
        return Done(
                tidspunkt,
                fodselsnummer,
                grupperingsid
        )
    }
}