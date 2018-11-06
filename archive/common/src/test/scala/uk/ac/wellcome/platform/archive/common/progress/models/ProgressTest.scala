package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  TimeTestFixture
}
import uk.ac.wellcome.storage.ObjectLocation

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

  it("can be created from a request display ingest") {
    val displayProvider = DisplayProvider("s3", "Amazon s3")
    val bucket = "ingest-bucket"
    val path = "bag.zip"
    val progressCreateRequest = RequestDisplayIngest(
      DisplayLocation(displayProvider, bucket, path),
      Some(
        DisplayCallback("http://www.wellcomecollection.org/callback/ok", None)),
      DisplayIngestType("create"),
      DisplayStorageSpace("space-id")
    )

    val progress = Progress(progressCreateRequest)

    progress.id shouldBe a[UUID]
    progress.sourceLocation shouldBe StorageLocation(
      StorageProvider(displayProvider.id),
      ObjectLocation(bucket, path))
    progress.callback shouldBe Some(
      Callback(URI.create(progressCreateRequest.callback.get.url)))
    progress.status shouldBe Progress.Initialised
    assertRecent(progress.createdDate)
    assertRecent(progress.lastModifiedDate)
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
