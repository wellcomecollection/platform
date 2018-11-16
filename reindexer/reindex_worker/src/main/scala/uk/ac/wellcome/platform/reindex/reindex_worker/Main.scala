package uk.ac.wellcome.platform.reindex.reindex_worker

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
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

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val scanSpecScanner = new ScanSpecScanner(
    dynamoDBClient = DynamoBuilder.buildDynamoClient(config)
  )

  val recordReader = new RecordReader(
    maxRecordsScanner = new MaxRecordsScanner(
      scanSpecScanner = scanSpecScanner,
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
    ),
    parallelScanner = new ParallelScanner(
      scanSpecScanner = scanSpecScanner,
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
    )
  )

  val hybridRecordSender = new BulkSNSSender(
    snsWriter = SNSBuilder.buildSNSWriter(config)
  )

  val workerService = new ReindexWorkerService(
    recordReader = recordReader,
    bulkSNSSender = hybridRecordSender,
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
  )

  run(workerService)
}
