package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.tms.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import org.apache.kafka.clients.consumer.ConsumerRecords

class InnboksInputEventService(
    private val varselForwarder: VarselForwarder,
) : EventBatchProcessorService<NokkelInput, InnboksInput> {
    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, InnboksInput>) {
        varselForwarder.processVarsler(events.map { it.key() to it.value() }, Eventtype.INNBOKS)
    }
}
