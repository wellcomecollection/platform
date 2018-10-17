package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Progress.Status
import uk.ac.wellcome.platform.archive.common.progress.models.progress._

trait ProgressGenerators extends RandomThings {

  def createProgress(): Progress = createProgressWith()

  val defaultUploadUri = new URI("s3://ingest-bucket/bag.zip")
  val defaultCallbackUri = new URI(
    "http://www.wellcomecollection.org/callback/ok")

  def createProgressWith(id: UUID = randomUUID,
                         uploadUri: URI = defaultUploadUri,
                         callback: Option[Callback] = Some(createCallback()),
                         space: Namespace = createSpace,
                         status: Status = Progress.Initialised,
                         resources: Seq[Resource] = List(createResource),
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

  def createProgressStatusUpdateWith(id: UUID,
                                     status: Status = Progress.Initialised,
                                     events: Seq[ProgressEvent] = List(
                                       createProgressEvent)): ProgressUpdate = {
    ProgressStatusUpdate(id, status, events)
  }

  def createSpace =
    Namespace(randomAlphanumeric())

  def createCallback(): Callback = createCallbackWith()

  def createCallbackWith(
    uri: URI = defaultCallbackUri,
    status: Callback.CallbackStatus = Callback.Pending): Callback =
    Callback(uri = uri, status = status)

  def createResource: Resource = {
    Resource(ResourceIdentifier(randomAlphanumeric(15)))
  }
}
