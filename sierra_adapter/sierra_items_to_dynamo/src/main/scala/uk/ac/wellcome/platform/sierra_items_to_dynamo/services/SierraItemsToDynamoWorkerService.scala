package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.parser.decode
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker}
import uk.ac.wellcome.circe._
import io.circe.generic.auto._
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

class SierraItemsToDynamoWorkerService @Inject()(
                                                  reader: SQSReader,
                                                  system: ActorSystem,
                                                  metrics: MetricsSender,
                                                  dynamoInserter: DynamoInserter
) extends SQSWorker(reader, system, metrics) {

  def processMessage(message: SQSMessage): Future[Unit] =
    decode[SierraRecord](message.body) match {
      case Right(record) => dynamoInserter.insertIntoDynamo(record.toItemRecord.get)
      case Left(e) =>
        Future {
          logger.warn(s"Failed processing $message", e)
          throw SQSReaderGracefulException(e)
        }
    }
}
