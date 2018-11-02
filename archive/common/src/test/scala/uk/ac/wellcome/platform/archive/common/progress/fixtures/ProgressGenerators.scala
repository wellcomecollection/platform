package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.Status
import uk.ac.wellcome.platform.archive.common.progress.models._

trait ProgressGenerators extends RandomThings {

  def createProgress(): Progress = createProgressWith()

  val testUploadUri = new URI("s3://ingest-bucket/bag.zip")
  val testCallbackUri =
    new URI("http://www.wellcomecollection.org/callback/ok")

  def createProgressWith(id: UUID = randomUUID,
                         uploadUri: URI = testUploadUri,
                         callback: Option[Callback] = Some(createCallback()),
                         space: Namespace = createSpace,
                         status: Status = Progress.Initialised,
                         resources: Seq[Resource] = List.empty,
                         events: List[ProgressEvent] = List.empty): Progress = {
    Progress(
      id = id,
      uploadUri = uploadUri,
      callback = callback,
      space = space,
      status = status,
      resources = resources,
      events = events)
  }

  def createProgressEvent: ProgressEvent = {
    ProgressEvent(randomAlphanumeric(15))
  }

  def createProgressEventUpdateWith(id: UUID,
                                    events: List[ProgressEvent] = List(
                                      createProgressEvent)) = {
    ProgressEventUpdate(id, events)
  }

  def createProgressStatusUpdateWith(
    id: UUID,
    status: Status = Progress.Initialised,
    resources: List[Resource] = List(createResource),
    events: Seq[ProgressEvent] = List(createProgressEvent)): ProgressUpdate = {
    ProgressStatusUpdate(id, status, resources, events)
  }

  def createSpace =
    Namespace(randomAlphanumeric())

  def createCallback(): Callback = createCallbackWith()

  def createCallbackWith(
    uri: URI = testCallbackUri,
    status: Callback.CallbackStatus = Callback.Pending): Callback =
    Callback(uri = uri, status = status)

  def createResource: Resource = {
    Resource(ResourceIdentifier(randomAlphanumeric(15)))
  }
}
