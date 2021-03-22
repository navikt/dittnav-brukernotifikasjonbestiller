CREATE TABLE IF NOT EXISTS brukernotifikasjonbestilling(
    eventid character varying(50),
    systembruker character varying(100),
    fodselsnummer character varying(50),
    brukernotifikasjontype character varying(50),
    eventtidspunkt timestamp without time zone
)