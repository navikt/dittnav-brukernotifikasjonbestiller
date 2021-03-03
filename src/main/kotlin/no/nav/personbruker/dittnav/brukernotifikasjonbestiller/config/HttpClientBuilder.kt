package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*

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