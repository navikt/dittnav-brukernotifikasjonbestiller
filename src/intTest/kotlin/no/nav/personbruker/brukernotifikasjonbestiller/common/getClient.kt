package no.nav.personbruker.brukernotifikasjonbestiller.common

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.buildJsonSerializer

fun getClient(producerNameAlias: String): HttpClient {
    return HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.encodedPath.contains("/producer/alias") && request.url.host.contains("event-handler")) {
                    respond(producerNameAlias, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                } else {
                    respondError(HttpStatusCode.BadRequest)
                }
            }
        }
        install(JsonFeature) {
            serializer = buildJsonSerializer()
        }
    }
}