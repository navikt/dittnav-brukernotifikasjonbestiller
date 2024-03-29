import default.*

object Avro: DependencyGroup {
    override val groupId get() = "io.confluent"
    override val version get() = "6.2.1"

    val avroSerializer get() = dependency("kafka-avro-serializer")
    val schemaRegistry get() = dependency("kafka-schema-registry")
}

object SulkyUlid: DependencyGroup {
    override val version get() = "8.2.0"
    override val groupId get() = "de.huxhorn.sulky"

    val sulkyUlid get() = dependency("de.huxhorn.sulky.ulid")
}

object Varsel: DependencyGroup {
    override val version get() = "1.0.1"
    override val groupId get() = "no.nav.tms.varsel"

    val kotlinBuilder get() = dependency("kotlin-builder")
}
