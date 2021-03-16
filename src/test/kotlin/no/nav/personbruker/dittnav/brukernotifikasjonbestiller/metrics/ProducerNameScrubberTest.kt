package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.jupiter.api.Test

internal class ProducerNameScrubberTest {
    private val systemUser = "x-dittnav"
    private val producerNameAlias = "dittnav"
    private val producerNameResolver = mockk<ProducerNameResolver>()
    private val nameScrubber = ProducerNameScrubber(producerNameResolver)

    @Test
    fun `should use available alias for producer if found`() {
        coEvery { producerNameResolver.getProducerNameAlias(systemUser) } returns producerNameAlias
        runBlocking {
            val scrubbedName = nameScrubber.getPublicAlias(systemUser)

            scrubbedName `should be equal to` producerNameAlias
            scrubbedName `should not be equal to` systemUser
        }
    }

    @Test
    fun `should use generic non system alias if not found and name resembles ident`() {
        val unknownSystemUser = "srvabcdefgh"
        coEvery { producerNameResolver.getProducerNameAlias(unknownSystemUser) } returns ""
        runBlocking {
            val scrubbedName = nameScrubber.getPublicAlias(unknownSystemUser)

            scrubbedName `should be equal to` nameScrubber.GENERIC_SYSTEM_USER
            scrubbedName `should not be equal to` systemUser
        }
    }

    @Test
    fun `should use generic system alias if not found`() {
        val unknownSystemUser = "dummy"
        coEvery { producerNameResolver.getProducerNameAlias(unknownSystemUser) } returns ""
        runBlocking {
            val scrubbedName = nameScrubber.getPublicAlias(unknownSystemUser)

            scrubbedName `should be equal to` nameScrubber.UNKNOWN_USER
            scrubbedName `should not be equal to` systemUser
        }
    }
}