package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class CreateULIDKtTest {

    @Test
    fun `Skal ha en leksikografisk rekkefolge paa naar ulid ble opprettet`() {
        runBlocking {
            val ulid_1 = createULID()
            delay(100)
            val ulid_2 = createULID()
            delay(100)
            val ulid_3 = createULID()

            val sortedUlids = mutableListOf(ulid_2, ulid_1, ulid_3)
            sortedUlids.sortBy { it }

            sortedUlids.get(0) shouldBe ulid_1
            sortedUlids.get(1) shouldBe ulid_2
            sortedUlids.get(2) shouldBe ulid_3
        }
    }

}