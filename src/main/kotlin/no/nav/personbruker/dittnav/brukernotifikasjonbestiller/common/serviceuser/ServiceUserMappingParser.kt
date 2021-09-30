package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser

object ServiceUserMappingParser {

    private const val VALID_CHARS = "[a-zA-Z0-9-]"
    private val MAPPING_PATTERN = "^($VALID_CHARS+)\\|($VALID_CHARS+):($VALID_CHARS+)$".toRegex()

    fun parseMappingStrings(mappingEntries: List<String>): Map<String, NamespaceAppName> {
        return mappingEntries.map { entry ->
            findValidMappingOrError(entry).destructured.let { (serviceUser, namespace, appName) ->
                serviceUser to NamespaceAppName(namespace, appName)
            }
        }.toMap()
    }

    private fun findValidMappingOrError(mappingString: String): MatchResult {
        val result = MAPPING_PATTERN.find(mappingString)

        return if (result != null) {
            result
        } else {
            throw ServiceUserMappingException("Fant ugyldig systembrukermapping. Gyldig format per innslag er [systembruker|namespace:appnavn]")
        }
    }
}
