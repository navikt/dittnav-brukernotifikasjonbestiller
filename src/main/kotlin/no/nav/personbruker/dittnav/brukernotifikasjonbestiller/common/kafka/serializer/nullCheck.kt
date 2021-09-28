package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.NokkelNullException
import org.apache.kafka.clients.consumer.ConsumerRecord

fun <T> ConsumerRecord<Nokkel, T>.getNonNullKey(): Nokkel {
    return key() ?: throw NokkelNullException("Produsenten har ikke spesifisert en kafka-key for sitt event")
}

fun <T> ConsumerRecord<NokkelLegacy, T>.getNonNullKey(): NokkelLegacy {
    return key() ?: throw NokkelNullException("Produsenten har ikke spesifisert en kafka-key for sitt event")
}
