package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

suspend inline fun <reified T> HttpClient.get(url: URL, systembruker: String): T = withContext(Dispatchers.IO) {
    request<T> {
        url(url)
        method = HttpMethod.Get
        parameter("systembruker", systembruker)
    }
}