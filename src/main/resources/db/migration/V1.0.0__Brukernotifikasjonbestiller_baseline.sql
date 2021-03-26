CREATE TABLE IF NOT EXISTS brukernotifikasjonbestilling(
    eventId character varying(50),
    systembruker character varying(100),
    eventtype character varying(50),
    eventtidspunkt timestamp without time zone
)