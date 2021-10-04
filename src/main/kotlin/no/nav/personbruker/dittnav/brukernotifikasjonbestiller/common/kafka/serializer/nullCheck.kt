package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import org.apache.kafka.clients.consumer.ConsumerRecord

fun <T> ConsumerRecord<NokkelInput, T>.getNonNullKey(): NokkelInput {
    return key() ?: throw NokkelNullException("Produsenten har ikke spesifisert en kafka-key for sitt event")
}

fun <T> ConsumerRecord<Nokkel, T>.getNonNullKey(): Nokkel {
    return key() ?: throw NokkelNullException("Produsenten har ikke spesifisert en kafka-key for sitt event")
}
