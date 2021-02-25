package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class DoneTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val doneExternal = AvroDoneObjectMother.createDone()

        val transformedNokkel = DoneTransformer.toNokkelInternal(nokkelExternal, doneExternal)
        val transformedDone = DoneTransformer.toDoneInternal(doneExternal)

        transformedNokkel.getFodselsnummer() `should be equal to` doneExternal.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` nokkelExternal.getEventId()

        transformedDone.getGrupperingsId() `should be equal to` doneExternal.getGrupperingsId()
        transformedDone.getTidspunkt() `should be equal to` doneExternal.getTidspunkt()

    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val doneExternal = AvroDoneObjectMother.createDoneWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(nokkelExternal, doneExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val doneExternal = AvroDoneObjectMother.createDoneWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(nokkelExternal, doneExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val doneExternal = AvroDoneObjectMother.createDone()

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(nokkelExternal, doneExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventid`() {
        val tooLongEventId = "1".repeat(51)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val doneExternal = AvroDoneObjectMother.createDone()

        invoking {
            runBlocking {
                DoneTransformer.toNokkelInternal(nokkelExternal, doneExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val doneExternal = AvroDoneObjectMother.createDoneWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                DoneTransformer.toDoneInternal(doneExternal)
            }
        } `should throw` FieldValidationException::class
    }

}