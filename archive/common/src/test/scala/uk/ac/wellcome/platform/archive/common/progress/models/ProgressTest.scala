package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{DisplayCallback, DisplayIngest, DisplayIngestType, DisplayStorageSpace}
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

  it("can be created from a display ingest") {
    val progressCreateRequest = DisplayIngest(
      id = None,
      "s3://ingest-bucket/bag.zip",
      Some(DisplayCallback( "http://www.wellcomecollection.org/callback/ok", None)),
      DisplayIngestType(),
      DisplayStorageSpace("space-id")
    )

    val progress = Progress(progressCreateRequest)

    progress.id shouldBe a[UUID]
    progress.uploadUri shouldBe URI.create(progressCreateRequest.uploadUrl)
    progress.callback shouldBe Some(
      Callback(URI.create(progressCreateRequest.callback.get.uri)))
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
    ("completed", Progress.Completed),
    ("failed", Progress.Failed),
  )

  it("parses all status values") {
    forAll(progressStatus) { (statusString, status) =>
      Progress.parseStatus(statusString) shouldBe status
    }
  }

  it("converts all callback status values to strings") {
    forAll(progressStatus) { (statusString, status) =>
      createProgressWith(status = status).status.toString shouldBe statusString
    }
  }

  it("throws if there is a parse error") {
    a[MatchError] should be thrownBy Progress.parseStatus("not-valid")
  }
}
