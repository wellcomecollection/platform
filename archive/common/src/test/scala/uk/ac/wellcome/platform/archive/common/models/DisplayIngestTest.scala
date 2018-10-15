package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent
}

class DisplayIngestTest extends FunSpec with Matchers {

  private val id = UUID.randomUUID()
  private val uploadUrl = "s3.example/key.zip"
  private val callbackUrl = "www.example.com/callback"
  private val space = "space-id"
  private val createdDate = "2018-10-10T09:38:55.321Z"
  private val modifiedDate = "2018-10-10T09:38:55.322Z"
  private val eventDate = "2018-10-10T09:38:55.323Z"
  private val eventDescription = "Event description"

  it("creates a DisplayIngest from Progress") {
    val progress: Progress = Progress(
      id,
      new URI(uploadUrl),
      Some(new URI(callbackUrl)),
      StorageSpace(space),
      Progress.Processing,
      Instant.parse(createdDate),
      Instant.parse(modifiedDate),
      List(ProgressEvent(eventDescription, Instant.parse(eventDate)))
    )

    val ingest = DisplayIngest(progress)

    ingest.id shouldBe id.toString
    ingest.uploadUrl shouldBe uploadUrl
    ingest.callbackUrl shouldBe Some(callbackUrl)
    ingest.status shouldBe DisplayIngestStatus("processing")
    ingest.createdDate shouldBe createdDate
    ingest.lastModifiedDate shouldBe modifiedDate
    ingest.events shouldBe List(
      DisplayProgressEvent(eventDescription, eventDate))
  }
}
