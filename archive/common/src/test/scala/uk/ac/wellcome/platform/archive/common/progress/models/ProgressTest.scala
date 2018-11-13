package uk.ac.wellcome.platform.archive.common.progress.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{ProgressGenerators, TimeTestFixture}

class ProgressTest
    extends FunSpec
    with Matchers
    with TimeTestFixture
    with ProgressGenerators
    with RandomThings {

  it("can be initialised") {
    val progress = createProgress
    progress.status shouldBe Progress.Initialised
    assertRecent(progress.createdDate)
    progress.lastModifiedDate shouldBe progress.createdDate
    progress.events shouldBe List.empty
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._

  private val progressStatus = Table(
    ("string-status", "parsed-status"),
    ("initialised", Progress.Initialised),
    ("processing", Progress.Processing),
    ("success", Progress.Completed),
    ("failure", Progress.Failed),
  )

  it("converts all callback status values to strings") {
    forAll(progressStatus) { (statusString, status) =>
      createProgressWith(status = status).status.toString shouldBe statusString
    }
  }
}
