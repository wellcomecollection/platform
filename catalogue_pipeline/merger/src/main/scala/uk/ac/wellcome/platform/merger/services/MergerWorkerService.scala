package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.MatcherResult
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
)(implicit context: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    Future.fromTry(fromJson[MatcherResult](message.Message)).map(_ => ())

  def stop() = system.terminate()
}
