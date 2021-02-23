package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID

object BeskjedTransformer {

    fun toBeskjedInternal(externalBeskjed: Beskjed): BeskjedIntern {
        return BeskjedIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(externalBeskjed.getTidspunkt())
                .setSynligFremTil(externalBeskjed.getSynligFremTil())
                .setGrupperingsId(externalBeskjed.getGrupperingsId())
                .setTekst(externalBeskjed.getTekst())
                .setLink(externalBeskjed.getLink())
                .setSikkerhetsnivaa(externalBeskjed.getSikkerhetsnivaa())
                .setEksternVarsling(externalBeskjed.getEksternVarsling())
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalBeskjed: Beskjed): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(externalNokkel.getEventId())
                .setSystembruker(externalNokkel.getSystembruker())
                .setFodselsnummer(externalBeskjed.getFodselsnummer())
                .build()
    }

}
