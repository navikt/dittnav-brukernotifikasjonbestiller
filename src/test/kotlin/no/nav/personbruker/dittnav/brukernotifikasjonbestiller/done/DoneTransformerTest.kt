package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class DoneTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalDone = AvroDoneObjectMother.createDone()

        val transformedNokkel = DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
        val transformedDone = DoneTransformer.toDoneInternal(externalDone)

        transformedNokkel.getFodselsnummer() `should be equal to` externalDone.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()

        transformedDone.getGrupperingsId() `should be equal to` externalDone.getGrupperingsId()
        transformedDone.getTidspunkt() `should be equal to` externalDone.getTidspunkt()

    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalDone = AvroDoneObjectMother.createDoneWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalDone = AvroDoneObjectMother.createDoneWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val externalDone = AvroDoneObjectMother.createDone()

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val externalDone = AvroDoneObjectMother.createDone()

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalDone = AvroDoneObjectMother.createDoneWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                DoneTransformer.toDoneInternal(externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

}
