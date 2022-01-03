package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.input.NokkelInput

object AvroNokkelInputObjectMother {


    private const val defaultEventId = "12345678-1234-1234-1234-1234567890ab"
    private const val defaultFodselsnr = "1234"
    private const val defaultGrupperingsid = "123"
    private const val defaultAppName = "defaultAppName"
    private const val defaultNamespace = "defaultNamespace"

    fun createNokkelInput(): NokkelInput {
        return NokkelInput(
            defaultEventId,
            defaultGrupperingsid,
            defaultFodselsnr,
            defaultNamespace,
            defaultAppName
        )
    }

    fun createNokkelInputWithEventId(eventId: String): NokkelInput {
        return NokkelInput(
            eventId,
            defaultGrupperingsid,
            defaultFodselsnr,
            defaultNamespace,
            defaultAppName
        )
    }

    fun createNokkelInputWithEventIdAndGroupId(eventId: String, groupId: String): NokkelInput {
        return NokkelInput(
            eventId,
            groupId,
            defaultFodselsnr,
            defaultNamespace,
            defaultAppName
        )
    }

    fun createNokkelInputWithEventIdAndFnr(eventId: String, fnr: String): NokkelInput {
        return NokkelInput(
            eventId,
            defaultGrupperingsid,
            fnr,
            defaultNamespace,
            defaultAppName
        )
    }
}
