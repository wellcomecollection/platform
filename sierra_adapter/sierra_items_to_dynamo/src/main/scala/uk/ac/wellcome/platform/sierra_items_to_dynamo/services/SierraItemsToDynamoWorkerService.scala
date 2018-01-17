package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.utils.GlobalExecutionContext._
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

class SierraItemsToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoInserter: DynamoInserter
) extends SQSWorker(reader, system, metrics) {

  def processMessage(message: SQSMessage): Future[Unit] =
    JsonUtil.fromJson[SierraRecord](message.body) match {
      case Success(record) =>
        dynamoInserter.insertIntoDynamo(record.toItemRecord.get)
      case Failure(e) =>
        Future {
          logger.warn(s"Failed processing $message", e)
          throw GracefulFailureException(e)
        }
    }
}
