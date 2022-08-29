package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.ZoneOffset

fun JsonNode.asTimestamp(): Long =
    LocalDateTime.parse(asText()).toInstant(ZoneOffset.UTC).toEpochMilli()