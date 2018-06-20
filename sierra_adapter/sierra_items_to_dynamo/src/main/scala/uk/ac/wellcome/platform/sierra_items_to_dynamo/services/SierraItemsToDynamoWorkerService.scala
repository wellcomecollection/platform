package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.storage.GlobalExecutionContext
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraItemsToDynamoWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  dynamoInserter: DynamoInserter
) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  implicit val ec: ExecutionContext = GlobalExecutionContext.context

  private def process(message: NotificationMessage): Future[Unit] = for {
    record <- Future.fromTry(fromJson[SierraRecord](message.Message))
    _ <- dynamoInserter.insertIntoDynamo(record.toItemRecord.get)
  } yield ()

  def stop() = system.terminate()
}
