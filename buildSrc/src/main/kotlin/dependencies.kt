import default.DependencyGroup

object BrukernotifikasjonSchemas: DependencyGroup {
    override val groupId = "com.github.navikt"

    val input = dependency("brukernotifikasjon-schemas", version = "1.2022.04.26-11.25-7155b5142c85")
    val internal = dependency("brukernotifikasjon-schemas-internal", version = "1.2022.04.27-11.14-a4039fef5785")
}
