package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

data class NamespaceAppName(
        val namespace: String,
        val appName: String
) {
        val concatenated get() = "$namespace:$appName"
}
