package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import io.circe.parser.decode
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker, WriteSQSToDynamo}
import uk.ac.wellcome.circe._
import io.circe.generic.auto._
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

class SierraItemsToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoInserter: DynamoInserter
) extends SQSWorker(reader, system, metrics)
    with WriteSQSToDynamo {

  private def insertItemRecord(record: SierraRecord) =
    dynamoInserter.insertIntoDynamo(record.toItemRecord.get)

  override def processMessage(message: SQSMessage): Future[Unit] =
    convertAndProcess(message, decode[SierraRecord], insertItemRecord)
}
