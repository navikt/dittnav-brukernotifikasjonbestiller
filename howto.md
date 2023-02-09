# Komme i gang med varsler

_For brukeropplevelsen er det viktig at du bruker riktig type varsel. Ta gjerne en ekstrasjekk
med [innholdsguiden vår](https://tms-dokumentasjon.intern.nav.no/innholdsguide)._

1. Kafka tilgang: Opprette en pull-request
   i [brukernotifikasjon-public-topic-iac](https://github.com/navikt/brukernotifikasjon-public-topic-iac).
2. Koble på topiceene.
3. Send event!

## Events

| navn  | beskrivelse | Deaktiveres av|
|---|---|---|
| Beskjed | For korte påminnelser eller oppdatering av status| bruker, eller av min side hvis synligFramTil dato er satt eller varslet har vært aktivt i mer enn ett år. | 
| Oppgave | For noe som må bli utført | Produsent når oppgavem er utført (med done event), eller av min side hvis synligFramTil dato er satt eller varslet har vært aktivt i mer enn ett år.  |
| Done | deaktiverer ett varsel| NA |

### Kafka key (NokkelInput)

Feltene i NokkelInput er som en kompositt-nøkkel for eventet. Alle feltene må være med for at eventet skal bli validert.

| felt  | beskrivelse |
|---|---|
| eventId |Unik identifikatore per event, må enten være en UUID eller en ULID |
| grupperingsId | deprecated, skal fjernes|
| fodselsnummer|Fødselsnummer til perseon som skal motta varselet.|
| namespace| namespacet til appen som har produsert eventet. Feltet har en begrensning på 63 tegn og kan ikke være null.|

[Nokkel-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/nokkelInput.avsc) på
Github.

## done-event

Produsent kan deaktivere ett varsel ved å sende `done-event` med samme eventId og fødselsnummer som varslet som skal
deaktiveres.\
Varslet vil fortsatt være synlig i varsel-historikk på min side.

### Topic

| Miljø  | Topic-navn  |
|---|---|
| dev | [aapen-brukernotifikasjon-done-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/dev-gcp/aapen-brukernotifikasjon-done.yaml) |
| prod | [aapen-brukernotifikasjon-done-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/prod-gcp/aapen-brukernotifikasjon-done.yaml) |

[DoneInput-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/doneInput.avsc) på
Github.

## Varseltyper

### Beskjed

Beskjed deaktiveres når personen har klikket på lenken, arkivert varslet, hvis synligFremTil dato er passert eller
varselt er eldre enn ett år.

#### topics

| Miljø  | Topic-navn  |
|---|---|
| dev | [aapen-brukernotifikasjon-beskjed-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/dev-gcp/aapen-brukernotifikasjon-beskjed.yaml) |
| prod | [aapen-brukernotifikasjon-beskjed-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/prod-gcp/aapen-brukernotifikasjon-beskjed.yaml) |

#### schema

Se [beskjedInput-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/beskjedInput.avsc)
på Github. Nærmere beskrivelse av verdier i feltene finner du lengre ned på denne siden.

### Oppgave

Oppgave deaktiveres når produsent har sendt done-event, hvis synligFremTil dato er passert eller varselt er eldre enn
ett år. \
**NB! Det er viktig å huske å sende done-event, hvis ikke vil det se ut for personen som at oppgaven ikke er utført.**

| Miljø  | Topic-navn  |
|---|---|
| dev | [aapen-brukernotifikasjon-oppgave-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/dev-gcp/aapen-brukernotifikasjon-oppgave.yaml) |
| prod | [aapen-brukernotifikasjon-oppgave-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/prod-gcp/aapen-brukernotifikasjon-oppgave.yaml) |

#### schemas

Se [oppgave-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/oppgaveInput.avsc) på
Github. \
Nærmere beskrivelse av verdier i feltene finner du lengre ned på denne siden.

### Feltbeskrivelser

| felt  | påkrevd  | beskrivelse  | restriksjoner  | tillegginfo |
|---|---| --- | --- | --- |
| tispunkt | ja (skal fjernes) | deprecated |  | |
| tekst | ja | Teksten som faktisk vises i varselet | Max 500 tegn| |
| link | ja | Lenke som blir aktivert når en person trykker på varselet entent i varselbjella, eller på min side| Komplett URL, inkludert `https` protokoll. Kan være tom string på beskjed.| |
| sikkerhetsnivaa | nei, default er 4 |påkrevd innlogingsnivå for å kunne se varselet| `3`, `4` | Min side støtter innlogging på nivå 3 (via MinId). Hvis personen har varsler med nivå 4 vil hen se type varsel, men ikke innholdet. |
| synligFremTil | nei | Tidspunkt for når varslet ikke lenger skal være synlig i varselbjella (legges i historikk) | UTC tidssone| synligFramTil = null -> synlig med mindre varselet arkiveres av bruker eller produsent sender ett done-event. |
| eksternVarsling | nei, default verdi er false | Om det skal sendes sms og/eller epost til personen | boolean | Om verdien er true sender min side en bestilling til Dokumentløsninger. For oppgaver bilr det satt en default revarsling på 7 dager. |
| prefererteKanaler | nei, default er EPOST | Ønskede varslingskanaler for ekstern varsling | `EPOST`,`SMS`| [Schema](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/java/no/nav/brukernotifikasjon/schemas/builders/domain/PreferertKanal.java) på Github |
| smsVarslingstekst| nei | Tekst som overstyrer standard SMS-tekst| Max 160 tegn.Skal ikke inneholde personopplysninger eller "dyplenker" | Standard SMS tekst for [oppgave](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/sms_oppgave.txt) og [beskjed](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/sms_beskjed.txt) |
| epostVarslingstekst | nei | Tekst som overstyrert standard tekst i [e-post mal](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/epost_mal.txt).| max 4000 tegn, HTML-tagger som er gyldig i `<body>`. Skal ikke inneholde personopplysninger eller "dyplenker"  | Standard epost tekst for [oppgave](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/epost_oppgave.txt) og [beskjed](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/epost_oppgave.txt)|
| epostVarslingstittel | nei | Tekst som overstyrer epostens standardtittel ("Du har fått en oppgave fra NAV") | Max 40 tegn. Skal ikke inneholde personopplysninger eller "dyplenker" | |


