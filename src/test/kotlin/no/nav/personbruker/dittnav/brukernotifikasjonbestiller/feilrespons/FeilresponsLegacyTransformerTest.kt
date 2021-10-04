package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import io.mockk.every
import io.mockk.mockk
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

internal class FeilresponsLegacyTransformerTest {
    private val mapper: ServiceUserMapper = mockk()

    private val namespace = "namespace"
    private val appName = "appName"

    private val transformer = FeilresponsLegacyTransformer(mapper)

    @Test
    fun `should transform from external to feilrespons`() {
        val eventtype = Eventtype.FEILRESPONS

        val nokkelExternal = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("eventId")
        val exception = FieldValidationException("Simulert feil i test.")

        every { mapper.getNamespaceAppName(nokkelExternal.getSystembruker()) } returns NamespaceAppName(namespace, appName)

        val (nokkelFeilrespons, feilrespons) = transformer.createFeilrespons(nokkelExternal.getEventId(), nokkelExternal.getSystembruker(), exception, eventtype)

        nokkelFeilrespons.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        nokkelFeilrespons.getEventId() `should be equal to` nokkelExternal.getEventId()
        nokkelFeilrespons.getBrukernotifikasjonstype() `should be equal to` eventtype.toString()
        nokkelFeilrespons.getNamespace() `should be equal to` namespace
        nokkelFeilrespons.getAppnavn() `should be equal to` appName
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.VALIDERINGSFEIL.toString()
    }

    @Test
    fun `should tolerate missing service user mapping`() {
        val eventtype = Eventtype.FEILRESPONS

        val nokkelExternal = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("eventId")
        val exception = FieldValidationException("Simulert feil i test.")

        every { mapper.getNamespaceAppName(nokkelExternal.getSystembruker()) } throws ServiceUserMappingException("")

        val (nokkelFeilrespons, feilrespons) = transformer.createFeilrespons(nokkelExternal.getEventId(), nokkelExternal.getSystembruker(), exception, eventtype)

        nokkelFeilrespons.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        nokkelFeilrespons.getEventId() `should be equal to` nokkelExternal.getEventId()
        nokkelFeilrespons.getBrukernotifikasjonstype() `should be equal to` eventtype.toString()
        nokkelFeilrespons.getNamespace() `should be equal to` "ukjent"
        nokkelFeilrespons.getAppnavn() `should be equal to` "ukjent"
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.VALIDERINGSFEIL.toString()
    }
}
