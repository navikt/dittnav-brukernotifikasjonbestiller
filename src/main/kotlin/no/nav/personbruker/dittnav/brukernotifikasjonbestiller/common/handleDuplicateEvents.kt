package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger(Brukernotifikasjonbestilling::class.java)

suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository): List<Brukernotifikasjonbestilling> {
    var result = emptyList<Brukernotifikasjonbestilling>()
    val duplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(successfullyValidatedEvents)

    if (duplicateEventIds.isNotEmpty()) {
        result = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(Eventtype.BESKJED, duplicateEventIds)
    }
    return result
}

fun addDuplicatesToProblematicEventsList(duplicateEvents: List<Brukernotifikasjonbestilling>, problematicEvents: MutableList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>, eventMetricsSession: EventMetricsSession) {
    duplicateEvents.forEach { duplicateEvent ->
        val duplicateEventException = DuplicateEventException("Dette eventet er allerede opprettet. Nokkel-en er et duplikat, derfor forkaster vi eventet.")
        val feilrespons = FeilresponsTransformer.createFeilrespons(duplicateEvent.eventId, duplicateEvent.systembruker, duplicateEventException, Eventtype.BESKJED)
        problematicEvents.add(feilrespons)
        logDuplicateEvent(eventMetricsSession, duplicateEvent)
    }
}

private fun logDuplicateEvent(eventMetricsSession: EventMetricsSession, duplicateEvent: Brukernotifikasjonbestilling) {
    log.info("${duplicateEvent.eventtype} med eventId: ${duplicateEvent.eventId} og systembruker: ${duplicateEvent.eventId} er et duplikat. Legger derfor ikke eventet på topic igjen.")
    eventMetricsSession.countDuplicateEventForSystemUser(duplicateEvent.systembruker)
}

suspend fun <T> sendRemainingValidatedEventsToInternalTopicAndPersistToDB(successfullyValidatedEvents: MutableMap<NokkelIntern, T>,
                                                                          duplicateEvents: List<Brukernotifikasjonbestilling>,
                                                                          kafkaProducer: KafkaProducerWrapper<NokkelIntern, T>,
                                                                          eventtype: Eventtype,
                                                                          brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {
    return if (duplicateEvents.isEmpty()) {
        produce(successfullyValidatedEvents, kafkaProducer, eventtype, brukernotifikasjonbestillingRepository)
    } else {
        val remainingValidatedEvents = getRemainingValidatedEvents(successfullyValidatedEvents, duplicateEvents, eventtype)
        produce(remainingValidatedEvents, kafkaProducer, eventtype, brukernotifikasjonbestillingRepository)
    }
}

fun <T> getRemainingValidatedEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, duplicateEvents: List<Brukernotifikasjonbestilling>, eventtype: Eventtype): Map<NokkelIntern, T> {
    return successfullyValidatedEvents
            .filter { successfullyValidatedEvent ->
                !duplicateEvents.any { duplicateEvent ->
                    duplicateEvent.eventId == successfullyValidatedEvent.key.getEventId()
                            && duplicateEvent.systembruker == successfullyValidatedEvent.key.getSystembruker()
                            && duplicateEvent.eventtype == eventtype.toString()
                }
            }
}

private suspend fun <T> produce(successfullyValidatedEvents: Map<NokkelIntern, T>,
                                kafkaProducer: KafkaProducerWrapper<NokkelIntern, T>,
                                eventtype: Eventtype,
                                brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {
    val eventsToSendKafka = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
    kafkaProducer.sendEvents(eventsToSendKafka)
    brukernotifikasjonbestillingRepository.persistInOneBatch(successfullyValidatedEvents, eventtype)
}
