package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

fun ObjectMapper.enableDittNavJsonConfig() {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}

val objectMapper = ObjectMapper().also { it.enableDittNavJsonConfig() }
