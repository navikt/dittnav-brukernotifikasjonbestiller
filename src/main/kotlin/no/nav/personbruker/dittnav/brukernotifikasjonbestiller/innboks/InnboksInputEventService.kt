package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.consumer.ConsumerRecords

class InnboksInputEventService(
    private val varselForwarder: VarselForwarder,
) : EventBatchProcessorService<NokkelInput, InnboksInput> {
    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, InnboksInput>) {
        events.map { it.key() to it.value() }
            .filter { it.second != null }
            .let { varselForwarder.processVarsler(it, Eventtype.INNBOKS) }
    }
}
