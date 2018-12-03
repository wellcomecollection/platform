package uk.ac.wellcome.platform.reindex.reindex_worker

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.WellcomeTypesafeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.DynamoBuilder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner,
  ScanSpecScanner
}
import uk.ac.wellcome.platform.reindex.reindex_worker.services.{
  BulkSNSSender,
  RecordReader,
  ReindexWorkerService
}

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val scanSpecScanner = new ScanSpecScanner(
      dynamoDBClient = DynamoBuilder.buildDynamoClient(config)
    )

    val recordReader = new RecordReader(
      maxRecordsScanner = new MaxRecordsScanner(
        scanSpecScanner = scanSpecScanner
      ),
      parallelScanner = new ParallelScanner(
        scanSpecScanner = scanSpecScanner
      )
    )

    val hybridRecordSender = new BulkSNSSender(
      snsMessageWriter = SNSBuilder.buildSNSMessageWriter(config)
    )

    new ReindexWorkerService(
      recordReader = recordReader,
      bulkSNSSender = hybridRecordSender,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config),
      snsConfig = SNSBuilder.buildSNSConfig(config)
    )
  }
}
