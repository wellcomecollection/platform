package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI
import java.util.UUID

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

  it("can be created from as create request") {
    val progressCreateRequest = ProgressCreateRequest(
      new URI("s3://ingest-bucket/bag.zip"),
      Some(new URI("http://www.wellcomecollection.org/callback/ok")),
      Namespace("space-id")
    )

    val progress = Progress(progressCreateRequest)

    progress.id shouldBe a[UUID]
    progress.uploadUri shouldBe progressCreateRequest.uploadUri
    progress.callback shouldBe Some(Callback(progressCreateRequest.callbackUri.get))
    progress.status shouldBe Progress.Initialised
    assertRecent(progress.createdDate)
    progress.lastModifiedDate shouldBe progress.createdDate
    progress.events shouldBe List.empty
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._

  private val progressStatus = Table(
    ("string-status", "parsed-status"),
    ("initialised",  Progress.Initialised),
    ("processing",   Progress.Processing),
    ("completed",    Progress.Completed),
    ("failed",       Progress.Failed),
  )

  it("parses all status values") {
    forAll (progressStatus) { (statusString, status) =>
      Progress.parseStatus(statusString) shouldBe status
    }
  }

  it("converts all callback status values to strings") {
    forAll (progressStatus) { (statusString, status) =>
      createProgressWith(status = status).status.toString shouldBe statusString
    }
  }

  it("throws if there is a parse error") {
    a [MatchError] should be thrownBy Progress.parseStatus("not-valid")
  }
}
