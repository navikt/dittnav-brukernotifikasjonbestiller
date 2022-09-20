package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.json.*

object HttpClientBuilder {

    fun build(): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = buildJsonSerializer()
            }
            install(HttpTimeout)
        }
    }

}