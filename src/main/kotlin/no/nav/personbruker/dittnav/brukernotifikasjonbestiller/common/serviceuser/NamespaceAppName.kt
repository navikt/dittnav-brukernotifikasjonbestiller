package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser

data class NamespaceAppName(
        val namespace: String,
        val appName: String
) {
        val concatenated get() = "$namespace:$appName"
}
