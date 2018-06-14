package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.MatcherResult
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class MergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[MatcherResult]
) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: MatcherResult): Future[Unit] =
    Future.successful(())

  def stop() = system.terminate()
}
