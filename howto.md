# Komme i gang med varsler

1. Kafka tilgang: Opprette en pull-request i [brukernotifikasjon-public-topic-iac](https://github.com/navikt/brukernotifikasjon-public-topic-iac).
2. Koble på topiceene.
3. Send event!

## Huskeliste
* For brukeropplevelsen er det viktig at du bruker riktig type varsel. Ta gjerne en ekstrasjekk med [innholdsguiden vår](https://tms-dokumentasjon.intern.nav.no/innholdsguide).
* Deaktiver(send done-event) når en oppgave er utført

## Varseltyper

### Beskjed
[BeskjedInput-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/beskjedInput.avsc) på Github. \
For mer info om meldingsinnholdet, se Feltbeskrivelser lengre ned på siden. \
Beskjed deaktiveres når bruker har klikket på lenken eller arkivert varslet.


#### topics
| Miljø  | Topic-navn  |
|---|---|
| dev | [aapen-brukernotifikasjon-beskjed-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/dev-gcp/aapen-brukernotifikasjon-beskjed.yaml) |
| prod | [aapen-brukernotifikasjon-beskjed-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/prod-gcp/aapen-brukernotifikasjon-beskjed.yaml) |

### Oppgave
[Oppgave-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/oppgaveInput.avsc) på Github. \
Mer info om felt og feltverdier, se Feltbeskrivelser lengre ned på siden. \
Det er viktig å huks å sende ett done-event når bruker har fullført oppgaven siden der er eneste måten varslet deaktiveres på.

| Miljø  | Topic-navn  |
|---|---|
| dev | [aapen-brukernotifikasjon-oppgave-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/dev-gcp/aapen-brukernotifikasjon-oppgave.yaml) |
| prod | [aapen-brukernotifikasjon-oppgave-v1](https://github.com/navikt/brukernotifikasjon-public-topic-iac/blob/main/prod-gcp/aapen-brukernotifikasjon-oppgave.yaml) |



## Feltbeskrivelser

### Kafka key (NokkelInput)
Feltene i NokkelInput er som en kompositt-nøkkel for å unikt identifisere eventet.
Alle feltene nedenfor må være med for at eventet skal bli validert.

* **eventId**: Unike identifikatoren per event, må enten være en UUID eller en ULID  
* **grupperingsId**(deprecated, skal fjernes)
* **fodselsnummer**: Fødselsnummer til perseon som skal motta varsel. 
* **namespace**: namespacet til appen som har produsert eventet. Feltet har en begrensning på 63 tegn og kan ikke være null.

[Nokkel-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/nokkelInput.avsc) på Github.

### Varselinnhold

#### Schemas
* [Beskjed-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/beskjedInput.avsc) på Github.
* [Oppgave-schemas](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/avro/oppgaveInput.avsc) på Github.

#### Felter

* **tidspunkt:** - deprecated
* **tekst:** - Teksten som faktisk vises i varselet. Feltet har en begrensning på 500 tegn og kan ikke være null.
* **link:** Lenke som blir aktivert når en person trykker på varselet entent i varselbjella, eller på min side. Må være en komplett URL, inkludert `https` protokoll. Kan være tom string på beskjed.
* **sikkerhetsnivaa (valgfri):**: Sikkerhetsnivået for informasjonen som eventet innholder. Se også [Mer om feltverdier](#Mer-om-feltverdier)
* **synligFremTil (valgfri):** Et tidspunkt på når varslet ikke lenger skal være synlig i varselbjella (legges i historikk). Bruk UTC som tidssone. Se også [Mer om feltverdier](#Mer-om-feltverdier)
* **eksternVarsling (valgfri):** Om det skal sendes sms og/eller epost til personen. Default-verdi er false.
* **prefererteKanaler (valgfri):** Ønskede varslingskanaler for ekstern varsling. Gyldige verdier finnes [her](https://github.com/navikt/brukernotifikasjon-schemas/blob/main/src/main/java/no/nav/brukernotifikasjon/schemas/builders/domain/PreferertKanal.java). Default-verdi er `EPOST`
* **smsVarslingstekst (valgfri):** Tekst som overstyrer SMS [standard tekst](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/sms_oppgave.txt) for ekstern varsling. Max 160 tegn.Skal ikke inneholde personopplysninger eller "dyplenker"
* **epostVarslingstekst (valgfri):** Tekst som overstyrer epost ([standard tekst](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/epost_oppgave.txt)) for ekstern varsling. Se også [Mer om feltverdier](#Mer-om-feltverdier)
* **epostVarslingstittel (valgfri):** Tekst som overstyrer epostens standardtittel ("Du har fått en oppgave fra NAV") for ekstern varsling. Teksten kan ikke være lengre enn 40 tegn.


#### Mer om feltverdier
##### sikkerhetsnivaa
Kan inneholde verdien `3` eller `4`. Default verdi er `4`.\
Min side støtter at en person er innlogget på nivå 3 (via MinId). Hvis hen har varsler med nivå 4 blir det maskert.
Personen ser bare hvilken type event dette er, men ikke noe av innholdet.
For å se innholdet må hen logge inn med ett høyere innloggingsnivå (via Idportens tjenester som støtter nivå 4, f.eks bankId).

##### synligFremTil 
Tidspunkt for når varselet ikke skal være synlig i varselbjella mer, f.eks beskjeden skal kun være synlig 7 dager.
Når synligFramTil-tidspunktet har passert deaktiverer min side varselet.
synligFramTil = null -> synlig med mindre varselet arkiveres av bruker eller produsent sender ett done-event.

#### epostVarslingstekst
Kun innhold av `<body>`([e-post mal](https://github.com/navikt/dittnav-varselbestiller/blob/main/src/main/resources/texts/epost_mal.txt)) skal overstyres.
Teksten kan innholde HTML tagger som er gyldig i `<body>` tag og den kan ikke være lengre enn 4,000 tegn.
Det er ikke tillatt å sette feltet dersom `eksternVarsling` er `false`. Skal ikke inneholde personopplysniger eller "dyplenker"
