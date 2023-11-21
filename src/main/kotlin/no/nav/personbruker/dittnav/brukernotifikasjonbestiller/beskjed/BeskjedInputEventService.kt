package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.tms.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.consumer.ConsumerRecords

class BeskjedInputEventService(
    private val varselForwarder: VarselForwarder
) : EventBatchProcessorService<NokkelInput, BeskjedInput> {
    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, BeskjedInput>) {
            varselForwarder.processVarsler(events.map { it.key() to it.value() }, Eventtype.BESKJED)
    }
}
