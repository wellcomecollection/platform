package uk.ac.wellcome.platform.archive.common.progress.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  TimeTestFixture
}

class ProgressTest
    extends FunSpec
    with Matchers
    with TimeTestFixture
    with ProgressGenerators
    with RandomThings {

  it("can be created") {
    val progress = createProgress
    progress.status shouldBe Progress.Accepted
    assertRecent(progress.createdDate)
    progress.lastModifiedDate shouldBe progress.createdDate
    progress.events shouldBe List.empty
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._

  private val progressStatus = Table(
    ("string-status", "parsed-status"),
    ("accepted", Progress.Accepted),
    ("processing", Progress.Processing),
    ("succeeded", Progress.Completed),
    ("failed", Progress.Failed),
  )

  it("converts all callback status values to strings") {
    forAll(progressStatus) { (statusString, status) =>
      createProgressWith(status = status).status.toString shouldBe statusString
    }
  }
}
