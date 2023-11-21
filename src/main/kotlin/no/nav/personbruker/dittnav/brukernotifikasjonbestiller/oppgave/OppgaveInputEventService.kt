package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.tms.brukernotifikasjon.schemas.input.OppgaveInput
import org.apache.kafka.clients.consumer.ConsumerRecords

class OppgaveInputEventService(
    private val varselForwarder: VarselForwarder,
) : EventBatchProcessorService<NokkelInput, OppgaveInput> {
    override suspend fun processEvents(events: ConsumerRecords<NokkelInput, OppgaveInput>) {
        varselForwarder.processVarsler(events.map { it.key() to it.value() }, Eventtype.OPPGAVE)
    }
}
