CREATE TABLE IF NOT EXISTS brukernotifikasjonbestilling(
    eventId character varying(50),
    systembruker character varying(100),
    eventtype character varying(50),
    prosesserttidspunkt timestamp without time zone
    );

ALTER TABLE BRUKERNOTIFIKASJONBESTILLING DROP CONSTRAINT IF EXISTS unikEventIdOgSystembrukerOgEventtype;
ALTER TABLE BRUKERNOTIFIKASJONBESTILLING ADD CONSTRAINT unikEventIdOgSystembrukerOgEventtype UNIQUE (eventId, systembruker, eventtype);
