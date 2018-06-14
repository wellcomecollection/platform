package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.matcher.MatcherResult

import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
  system: ActorSystem,
  messageStream: MessageStream[MatcherResult]
)(implicit ec: ExecutionContext) {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: MatcherResult): Future[Unit] =
    Future.successful(())

  def stop() = system.terminate()
}
