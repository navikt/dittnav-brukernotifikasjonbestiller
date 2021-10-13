package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser

class ServiceUserMapper(private val mappings: Map<String, NamespaceAppName>) {

    fun getNamespaceAppName(serviceUser: String): NamespaceAppName {
        val namespaceAppName = mappings[serviceUser]

        return if (namespaceAppName != null) {
            namespaceAppName
        } else {
            throwMappingException(serviceUser)
        }
    }

    private fun throwMappingException(serviceUser: String): Nothing {
        val maskedUser = "${"*".repeat(serviceUser.length - 3)}${serviceUser.substring(serviceUser.length - 3)}"

        throw ServiceUserMappingException("Fant ikke mapping for systembruker $maskedUser.")
    }
}

