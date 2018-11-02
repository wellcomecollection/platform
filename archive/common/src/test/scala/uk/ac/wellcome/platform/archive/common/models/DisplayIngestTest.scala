package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.storage.ObjectLocation

class DisplayIngestTest extends FunSpec with Matchers with RandomThings {

  private val id = UUID.randomUUID()
  private val callbackUrl = "http://www.example.com/callback"
  private val spaceId = "space-id"
  private val createdDate = "2018-10-10T09:38:55.321Z"
  private val modifiedDate = "2018-10-10T09:38:55.322Z"
  private val eventDate = "2018-10-10T09:38:55.323Z"
  private val eventDescription = "Event description"

  it("creates a DisplayIngest from Progress") {
    val bagId = randomBagId
    val progress: Progress = Progress(
      id,
      StorageLocation(
        StorageProvider("s3"),
        ObjectLocation("bukkit", "key.txt")),
      Namespace(spaceId),
      Some(Callback(new URI(callbackUrl))),
      Progress.Processing,
      Some(bagId),
      Instant.parse(createdDate),
      Instant.parse(modifiedDate),
      List(ProgressEvent(eventDescription, Instant.parse(eventDate)))
    )

    val ingest = ResponseDisplayIngest(progress)
    println(toJson(ingest))

    ingest.id shouldBe id
    ingest.sourceLocation shouldBe DisplayLocation(DisplayProvider("s3"), bucket = "bukkit", path = "key.txt")
    ingest.callback shouldBe Some(
      DisplayCallback(callbackUrl, Some(ingest.callback.get.status.get)))
    ingest.space shouldBe DisplayStorageSpace(spaceId)
    ingest.status shouldBe DisplayStatus("processing")
    ingest.bag shouldBe Some(
      IngestDisplayBag(s"${bagId.space}/${bagId.externalIdentifier}"))
    ingest.createdDate shouldBe createdDate
    ingest.lastModifiedDate shouldBe modifiedDate
    ingest.events shouldBe List(
      DisplayProgressEvent(eventDescription, eventDate))
  }
}
