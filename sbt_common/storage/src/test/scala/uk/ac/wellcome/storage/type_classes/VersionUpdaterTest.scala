package uk.ac.wellcome.storage.type_classes

import org.scalatest.{FunSpec, Matchers}
import shapeless.LabelledGeneric

class VersionUpdaterTest extends FunSpec with Matchers {

  case class TestVersion(something: String,
                         version: Int,
                         somethingElse: String)

  it("creates a VersionUpdater instance for an hlist") {
    val gen = LabelledGeneric[TestVersion]

    val versionUpdater = VersionUpdater[gen.Repr]

    val version = 1
    val originalTestVersion =
      TestVersion("something", version, "somethingElse")
    val newVersion = 3
    versionUpdater.updateVersion(gen.to(originalTestVersion), newVersion) shouldBe gen
      .to(originalTestVersion.copy(version = newVersion))
  }

  it("creates a VersionUpdater instance for a case class") {
    val versionUpdater = VersionUpdater[TestVersion]

    val version = 1
    val originalTestVersion =
      TestVersion("something", version, "somethingElse")
    val newVersion = 3
    versionUpdater.updateVersion(originalTestVersion, newVersion) shouldBe originalTestVersion
      .copy(version = newVersion)
  }
}
