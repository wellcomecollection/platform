package uk.ac.wellcome.type_classes

import org.scalatest.{FunSpec, Matchers}
import shapeless.LabelledGeneric

class VersionGetterTest extends FunSpec with Matchers {

  case class TestVersion(something: String,
                         version: Int,
                         somethingElse: String)

  it("creates a VersionGetter instance for an hlist") {
    val gen = LabelledGeneric[TestVersion]

    val versionGetter = VersionGetter[gen.Repr]

    val version = 1
    versionGetter.version(gen.to(
      TestVersion("something", version, "somethingElse"))) shouldBe version
  }

  it("creates a VersionGetter instance for a case class") {
    val versionGetter = VersionGetter[TestVersion]

    val version = 1
    versionGetter.version(TestVersion("something", version, "somethingElse")) shouldBe version
  }
}
