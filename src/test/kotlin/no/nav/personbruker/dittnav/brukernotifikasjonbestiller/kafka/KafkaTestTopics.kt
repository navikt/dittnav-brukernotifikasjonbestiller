package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka

object KafkaTestTopics {
    const val beskjedInputTopicName = "inputBeskjedTopic"
    const val oppgaveInputTopicName = "inputOppgaveTopic"
    const val innboksInputTopicName = "inputInnboksTopic"
    const val doneInputTopicName = "inputDoneTopic"
    val inputTopics = listOf(
        beskjedInputTopicName,
        oppgaveInputTopicName,
        innboksInputTopicName,
        doneInputTopicName
    )
}
