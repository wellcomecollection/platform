package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sqs.SQSWorkerToDynamo
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.messaging.sqs.SQSReader
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.utils.GlobalExecutionContext._
import io.circe.generic.extras.semiauto._
import scala.concurrent.Future
import uk.ac.wellcome.dynamo._

class SierraItemsToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoInserter: DynamoInserter
) extends SQSWorkerToDynamo[SierraRecord](reader, system, metrics) {

  override implicit val decoder = deriveDecoder[SierraRecord]

  override def store(record: SierraRecord): Future[Unit] =
    dynamoInserter.insertIntoDynamo(record.toItemRecord.get)
}
