package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should have key`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class ServiceUserMappingParserTest {

    @Test
    fun `skal mappe gyldige mapping-strenger riktig`() {
        val serviceUser1 = "serviceUser1"
        val namespace1 = "namespace1"
        val appName1 = "appName1"
        val serviceUser2 = "serviceUser2"
        val namespace2 = "namespace2"
        val appName2 = "appName2"

        val mappingString1 = "$serviceUser1|$namespace1:$appName1"
        val mappingString2 = "$serviceUser2|$namespace2:$appName2"

        val mappings = listOf(mappingString1, mappingString2)

        val parsed = ServiceUserMappingParser.parseMappingStrings(mappings)

        parsed `should have key` serviceUser1
        parsed `should have key` serviceUser2

        parsed[serviceUser1]?.namespace `should be equal to` namespace1
        parsed[serviceUser1]?.appName `should be equal to` appName1
        parsed[serviceUser2]?.namespace `should be equal to` namespace2
        parsed[serviceUser2]?.appName `should be equal to` appName2
    }

    @Test
    fun `skal kaste exception dersom det finnes en ugyldig mapping`() {
        val mappingString = "DoesNotLookLikeACorrectMapping"

        invoking {
            ServiceUserMappingParser.parseMappingStrings(listOf(mappingString))
        } `should throw` ServiceUserMappingException::class
    }
}
