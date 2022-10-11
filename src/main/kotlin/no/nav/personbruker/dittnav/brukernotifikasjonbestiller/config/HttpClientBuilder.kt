package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config


import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson

object HttpClientBuilder {

    fun build(): HttpClient {
        return HttpClient(Apache) {
            install(ContentNegotiation) {
                jackson {
                    enableDittNavJsonConfig()
                }
            }
            install(HttpTimeout)
        }
    }

}