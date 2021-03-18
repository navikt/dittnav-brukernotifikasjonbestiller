package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics


import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.buildJsonSerializer
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL

internal class ProducerNameResolverTest {

    private val systembruker = "x-dittnav"
    private val produsent = "dittnav"
    private val eventHandlerBaseURL = URL("http://event-handler")

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `Skal hente produsentnavn alias`() {
        val client = getClient(produsent)

        val producerNameResolver = ProducerNameResolver(client, eventHandlerBaseURL)

        runBlocking {
            val producerNameAlias = producerNameResolver.getProducerNameAlias(systembruker)
            producerNameAlias `should be equal to` produsent
        }
    }

    @Test
    fun `skal returnere tom String hvis produsentnavn ikke ble funnet i handler`() {
        val client = getClient("")

        val producerNameResolver = ProducerNameResolver(client, eventHandlerBaseURL)

        runBlocking {
            val producerNameAlias = producerNameResolver.getProducerNameAlias(systembruker)
            producerNameAlias `should be equal to` ""
        }
    }

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

}