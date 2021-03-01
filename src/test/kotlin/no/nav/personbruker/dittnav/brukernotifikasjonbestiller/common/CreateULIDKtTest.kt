package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.`should be equal to`
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

            sortedUlids.get(0) `should be equal to` ulid_1
            sortedUlids.get(1) `should be equal to` ulid_2
            sortedUlids.get(2) `should be equal to` ulid_3
        }
    }

}