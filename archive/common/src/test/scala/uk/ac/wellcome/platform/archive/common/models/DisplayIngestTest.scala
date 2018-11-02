package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.storage.ObjectLocation

class DisplayIngestTest extends FunSpec with Matchers {

  private val id = UUID.randomUUID()
  private val callbackUrl = "http://www.example.com/callback"
  private val spaceId = "space-id"
  private val resourceId = "bag-id"
  private val createdDate = "2018-10-10T09:38:55.321Z"
  private val modifiedDate = "2018-10-10T09:38:55.322Z"
  private val eventDate = "2018-10-10T09:38:55.323Z"
  private val eventDescription = "Event description"

  it("creates a DisplayIngest from Progress") {
    val progress: Progress = Progress(
      id,
      StorageLocation(StorageProvider("s3", "Amazon S3"), ObjectLocation("bukkit", "key.txt")),
      Namespace(spaceId),
      Some(Callback(new URI(callbackUrl))),
      Progress.Processing,
      List(Resource(ResourceIdentifier(resourceId))),
      Instant.parse(createdDate),
      Instant.parse(modifiedDate),
      List(ProgressEvent(eventDescription, Instant.parse(eventDate)))
    )

    val ingest = ResponseDisplayIngest(progress)
    println(toJson(ingest))

    ingest.id shouldBe id
    ingest.sourceLocation shouldBe DisplayLocation(DisplayProvider("s3", "Amazon S3"), bucket = "bukkit", path = "key.txt")
    ingest.callback shouldBe Some(
      DisplayCallback(
        callbackUrl,
        Some(ingest.callback.get.status.get)))
    ingest.space shouldBe DisplayStorageSpace(spaceId)
    ingest.status shouldBe DisplayStatus("processing")
    ingest.resources shouldBe List(DisplayIngestResource(resourceId))
    ingest.createdDate shouldBe createdDate
    ingest.lastModifiedDate shouldBe modifiedDate
    ingest.events shouldBe List(
      DisplayProgressEvent(eventDescription, eventDate))
  }
}
