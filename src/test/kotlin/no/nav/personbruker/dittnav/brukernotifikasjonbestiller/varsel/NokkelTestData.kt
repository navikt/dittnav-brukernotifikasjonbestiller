package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import no.nav.brukernotifikasjon.schemas.input.NokkelInput

object NokkelTestData {
    private const val defaultFodselsnr = "12345678910"
    private const val defaultGrupperingsid = "123"
    private const val defaultAppName = "defaultAppName"
    private const val defaultNamespace = "defaultNamespace"

    fun nokkel(
        eventId: String? = "12345678-1234-1234-1234-1234567890ab",
        fodselsnummer: String? = "12345678910",
        grupperingsid: String? = "123",
        appnavn: String? = "defaultAppName",
        namespace: String? = "defaultNamespace",
    ): NokkelInput {
        return NokkelInput(
            eventId,
            grupperingsid,
            fodselsnummer,
            namespace,
            appnavn
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
}
