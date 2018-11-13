package uk.ac.wellcome.platform.merger.fixtures

import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.work.internal.{BaseWork, TransformedBaseWork}
import uk.ac.wellcome.platform.merger.services.{Merger, MergerManager, MergerWorkerService, RecorderPlaybackService}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

trait WorkerServiceFixture {
  type TransformedBaseWorkStore = VersionedHybridStore[
    TransformedBaseWork,
    EmptyMetadata,
    ObjectStore[TransformedBaseWork]]

  def withWorkerService[R](
    sqsStream: SQSStream[NotificationMessage],
    vhs: TransformedBaseWorkStore,
    messageWriter: MessageWriter[BaseWork])(
    testWith: TestWith[MergerWorkerService, R]): R = {
    val workerService = new MergerWorkerService(
      sqsStream = sqsStream,
      playbackService = new RecorderPlaybackService(vhs),
      mergerManager = new MergerManager(new Merger()),
      messageWriter = messageWriter
    )

    testWith(workerService)
  }
}
