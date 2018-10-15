package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.TimeTestFixture

import scala.util.Random

class ProgressTest extends FunSpec with Matchers with TimeTestFixture {
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
      Some(new URI("http://www.wellcomecollection.org/callback/ok")))

    val progress = Progress(progressCreateRequest)

    progress.id shouldBe a[UUID]
    progress.uploadUri shouldBe progressCreateRequest.uploadUri
    progress.callback shouldBe Some(Callback(progressCreateRequest.callbackUri.get))
    progress.status shouldBe Progress.Initialised
    assertRecent(progress.createdDate)
    progress.lastModifiedDate shouldBe progress.createdDate
    progress.events shouldBe List.empty
  }

  it("updates from a progress update") {
    val progress = createProgressWith(events = List(createProgressEvent))

    val progressUpdate = ProgressEventUpdate(List(createProgressEvent))
    val updatedProgress = progress.update(progressUpdate)

    updatedProgress.events should contain theSameElementsAs progress.events ++ progressUpdate.events
    assertAllRecent(updatedProgress.events.map(_.createdDate))
  }

  it("updates from a progress status update") {
    val progress = createProgressWith(events = List(createProgressEvent))

    val progressUpdate = ProgressStatusUpdate(Progress.Completed, List(createProgressEvent))
    val updatedProgress = progress.update(progressUpdate)

    updatedProgress.status shouldBe Progress.Completed
    updatedProgress.events should contain theSameElementsAs progress.events ++ progressUpdate.events
    assertAllRecent(updatedProgress.events.map(_.createdDate))
  }

  it("updates from a progress resource update") {
    val progress = createProgressWith(events = List(createProgressEvent))

    val progressUpdate = ProgressResourceUpdate(
      List(createResource),
      List(createProgressEvent))
    val updatedProgress = progress.update(progressUpdate)

    updatedProgress.resources should contain theSameElementsAs progressUpdate.affectedResources
    updatedProgress.events should contain theSameElementsAs progress.events ++ progressUpdate.events
    assertAllRecent(updatedProgress.events.map(_.createdDate))
  }

  it("updates from a progress callback status update") {
    val progress = createProgressWith(events = List(createProgressEvent))

    val progressUpdate = ProgressCallbackStatusUpdate(
      Callback.Succeeded,
      List(ProgressEvent("another event")))
    val updatedProgress = progress.update(progressUpdate)

    updatedProgress.callback shouldBe Some(progress.callback.get.copy(callbackStatus = Callback.Succeeded))
    updatedProgress.events should contain theSameElementsAs progress.events ++ progressUpdate.events
    assertAllRecent(updatedProgress.events.map(_.createdDate))
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

  it("throws if there is a parse error") {
    a [MatchError] should be thrownBy Progress.parseStatus("not-valid")
  }

  private def createProgress = {
    Progress(
      UUID.randomUUID(),
      new URI("s3://ingest-bucket/bag.zip"),
      Some(Callback(new URI("http://www.wellcomecollection.org/callback/ok")))
    )
  }

  private def createProgressWith(events: List[ProgressEvent]) = {
    createProgress.copy(events = events)
  }

  private def createProgressEvent = {
    ProgressEvent(randomAlphanumeric(15))
  }

  private def createResource = {
    Resource(ResourceIdentifier(randomAlphanumeric(15)))
  }

  def randomAlphanumeric(length: Int): String =
    Random.alphanumeric take length mkString
}
