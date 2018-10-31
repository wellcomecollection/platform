package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models._

class DisplayIngestTest extends FunSpec with Matchers {

  private val id = UUID.randomUUID()
  private val uploadUrl = "s3://example/key.zip"
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
      new URI(uploadUrl),
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
    ingest.uploadUrl shouldBe uploadUrl
    ingest.callback shouldBe Some(
      DisplayCallback(
        callbackUrl,
        Some(ingest.callback.get.status.get.toString)))
    ingest.space shouldBe DisplayStorageSpace(spaceId)
    ingest.status shouldBe DisplayIngestStatus("processing")
    ingest.resources shouldBe List(DisplayIngestResource(resourceId))
    ingest.createdDate shouldBe createdDate
    ingest.lastModifiedDate shouldBe modifiedDate
    ingest.events shouldBe List(
      DisplayProgressEvent(eventDescription, eventDate))
  }
}
