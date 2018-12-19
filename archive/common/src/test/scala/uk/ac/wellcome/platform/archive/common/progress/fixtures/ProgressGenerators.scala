package uk.ac.wellcome.platform.archive.common.progress.fixtures

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.BagId
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.Status
import uk.ac.wellcome.platform.archive.common.progress.models.{
  StorageLocation,
  _
}
import uk.ac.wellcome.storage.ObjectLocation

trait ProgressGenerators extends RandomThings {

  val storageLocation = StorageLocation(
    StandardStorageProvider,
    ObjectLocation(randomAlphanumeric(), randomAlphanumeric()))

  def createProgress: Progress = createProgressWith()

  val testCallbackUri =
    new URI("http://www.wellcomecollection.org/callback/ok")

  def createProgressWith(id: UUID = randomUUID,
                         sourceLocation: StorageLocation = storageLocation,
                         callback: Option[Callback] = Some(createCallback()),
                         space: Namespace = createSpace,
                         status: Status = Progress.Accepted,
                         maybeBag: Option[BagId] = None,
                         events: List[ProgressEvent] = List.empty): Progress = {
    Progress(
      id = id,
      sourceLocation = sourceLocation,
      callback = callback,
      space = space,
      status = status,
      bag = maybeBag,
      events = events)
  }

  def createProgressEvent: ProgressEvent = {
    ProgressEvent(randomAlphanumeric(15))
  }

  def createProgressEventUpdateWith(id: UUID = randomUUID,
                                    events: List[ProgressEvent] = List(
                                      createProgressEvent))
    : ProgressEventUpdate =
    ProgressEventUpdate(id, events)

  def createProgressEventUpdate: ProgressEventUpdate =
    createProgressEventUpdateWith()

  def createProgressStatusUpdateWith(
    id: UUID,
    status: Status = Progress.Accepted,
    maybeBag: Option[BagId] = Some(randomBagId),
    events: Seq[ProgressEvent] = List(createProgressEvent)): ProgressUpdate = {
    ProgressStatusUpdate(id, status, maybeBag, events)
  }

  def createSpace = Namespace(randomAlphanumeric())

  def createCallback(): Callback = createCallbackWith()

  def createCallbackWith(
    uri: URI = testCallbackUri,
    status: Callback.CallbackStatus = Callback.Pending): Callback =
    Callback(uri = uri, status = status)

}
