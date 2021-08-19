package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import io.ktor.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.get
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime

class ProducerNameResolver(private val client: HttpClient, eventHandlerBaseURL: URL) {

    private val pathToEndpoint: URL = URL("$eventHandlerBaseURL/producer/alias")
    private var producerNameAliases = mutableMapOf<String, String>()
    private var lastRetrievedFromHandler: LocalDateTime? = null
    private val CHECK_PRODUCERNAME_CACHE_IN_MINUTES = 60

    private val log = LoggerFactory.getLogger(ProducerNameResolver::class.java)

    suspend fun getProducerNameAlias(systembruker: String): String? {
        updateCachedMapIfNeeded(systembruker)
        return producerNameAliases[systembruker]
    }

    private suspend fun updateCachedMapIfNeeded(systembruker: String) {
        if (shouldFetchNewValuesFromHandler(systembruker)) {
            log.info("Periodisk oppdatering av produsent-cache.")
            updateCachedMap(systembruker)

        } else {
            val containsAlias = producerNameAliases.containsKey(systembruker)
            if (!containsAlias) {
                log.info("Manglet alias for '$systembruker'. Er det kanskje ikke mapping for denne systembrukeren?")
            }
        }
    }

    private fun shouldFetchNewValuesFromHandler(systembruker: String): Boolean {
        return producerNameAliases[systembruker].isNullOrBlank() ||
                lastRetrievedFromHandler == null ||
                Math.abs(Duration.between(lastRetrievedFromHandler, LocalDateTime.now()).toMinutes()) > CHECK_PRODUCERNAME_CACHE_IN_MINUTES
    }

    private suspend fun updateCachedMap(systembruker: String) = withContext(Dispatchers.IO) {
        val alias = getProducerNameAliasFromHandler(systembruker)
        producerNameAliases[systembruker] = alias
        lastRetrievedFromHandler = LocalDateTime.now()
    }

    private suspend fun getProducerNameAliasFromHandler(systembruker: String): String {
        try {
            return client.get(pathToEndpoint, systembruker)
        } catch (e: Exception) {
            return "alias_unavailable"
        }

    }

}