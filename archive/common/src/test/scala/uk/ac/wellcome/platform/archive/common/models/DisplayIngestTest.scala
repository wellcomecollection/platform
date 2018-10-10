package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent}

class DisplayIngestTest extends FunSpec with Matchers {

  it("creates a DisplayIngest from Progress") {
    val id = UUID.randomUUID()
    val uploadUrl = "s3.example/key.zip"
    val callbackUrl = "www.example.com/callback"
    val createdDate = "2018-10-10T09:38:55.321Z"
    val modifiedDate = "2018-10-10T09:38:55.322Z"
    val eventDate = "2018-10-10T09:38:55.323Z"
    val eventDescription = "Event description"

    val progress: Progress = Progress(
      id,
      new URI(uploadUrl),
      Some(new URI(callbackUrl)),
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
