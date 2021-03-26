package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedEventService
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

private val log: Logger = LoggerFactory.getLogger(BeskjedEventService::class.java)

suspend fun <T> getDuplicateEvents(successfullyValidatedEvents: MutableMap<NokkelIntern, T>, brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository): List<Brukernotifikasjonbestilling> {
    var duplicateResult = emptyList<Brukernotifikasjonbestilling>()
    val isDuplicateEventIds = brukernotifikasjonbestillingRepository.fetchEventsFromEventId(successfullyValidatedEvents)

    if (isDuplicateEventIds.isNotEmpty()) {
        duplicateResult = brukernotifikasjonbestillingRepository.fetchEventsIfDuplicate(isDuplicateEventIds, Eventtype.BESKJED)
    }
    return duplicateResult
}

fun addDuplicatesToProblematicEventsList(duplicateEvents: List<Brukernotifikasjonbestilling>, problematicEvents: MutableList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>, eventMetricsSession: EventMetricsSession) {
    if (duplicateEvents.isNotEmpty()) {
        duplicateEvents.forEach { duplicateEvent ->
            val duplicateEventException = DuplicateEventException("Dette eventet finnes allerede. Nokkel-en er et duplikat.")
            val feilrespons = FeilresponsTransformer.createFeilrespons(duplicateEvent.eventid, duplicateEvent.systembruker, duplicateEventException, Eventtype.BESKJED)
            problematicEvents.add(feilrespons)
            logDuplicateEvent(eventMetricsSession, duplicateEvent)
        }
    }

}

suspend fun <T> sendRemainingValidatedEventsToInternalTopicAndPersistToDB(successfullyValidatedEvents: MutableMap<NokkelIntern, T>,
                                                                          duplicateEvents: List<Brukernotifikasjonbestilling>,
                                                                          internalEventProducer: KafkaProducerWrapper<NokkelIntern, T>,
                                                                          eventtype: Eventtype,
                                                                          brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {
    return if (duplicateEvents.isEmpty()) {
        produce(successfullyValidatedEvents, internalEventProducer, eventtype, brukernotifikasjonbestillingRepository)
    } else {
        //TODO: se på filter
        val remainingValidatedEvents = successfullyValidatedEvents.filter { nokkel -> !duplicateEvents.contains(nokkel.key.getEventId()) }
        produce(remainingValidatedEvents, internalEventProducer, eventtype, brukernotifikasjonbestillingRepository)
    }
}

private fun logDuplicateEvent(eventMetricsSession: EventMetricsSession, duplicateEvent: Brukernotifikasjonbestilling) {
    log.info("Event med eventId: ${duplicateEvent.eventid}, systembruker: ${duplicateEvent.eventid} og eventtype: ${duplicateEvent.eventtype} er et duplikat. Legger derfor ikke eventet på topic igjen.")
    eventMetricsSession.countDuplicateEventForSystemUser(duplicateEvent.systembruker)
}

private suspend fun <T> produce(successfullyValidatedEvents: Map<NokkelIntern, T>,
                                internalEventProducer: KafkaProducerWrapper<NokkelIntern, T>,
                                eventtype: Eventtype,
                                brukernotifikasjonbestillingRepository: BrukernotifikasjonbestillingRepository) {
    val events = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
    internalEventProducer.sendEvents(events)
    brukernotifikasjonbestillingRepository.persistInOneBatch(successfullyValidatedEvents, eventtype)
}

