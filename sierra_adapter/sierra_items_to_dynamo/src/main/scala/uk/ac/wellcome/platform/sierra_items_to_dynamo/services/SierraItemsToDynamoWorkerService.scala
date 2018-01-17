package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
<<<<<<< HEAD
import io.circe
import io.circe.parser.decode
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker, SQSWorkerToDynamo}
import uk.ac.wellcome.circe._
import io.circe.generic.auto._
=======
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.exceptions.GracefulFailureException
>>>>>>> 7b110a8676de41618b8a7367ed4d0ca325463775
import uk.ac.wellcome.utils.GlobalExecutionContext._
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

class SierraItemsToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoInserter: DynamoInserter
) extends SQSWorkerToDynamo[SierraRecord](reader, system, metrics) {

  override def conversion(s: String): Either[circe.Error, SierraRecord] =
    decode[SierraRecord](s)

  override def store(record: SierraRecord): Future[Unit] =
    dynamoInserter.insertIntoDynamo(record.toItemRecord.get)

<<<<<<< HEAD
=======
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
>>>>>>> 7b110a8676de41618b8a7367ed4d0ca325463775
}
