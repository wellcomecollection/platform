package uk.ac.wellcome.platform.reindex.reindex_worker

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
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

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
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
    snsMessageWriter = SNSBuilder.buildSNSMessageWriter(config)
  )

  val workerService = new ReindexWorkerService(
    recordReader = recordReader,
    bulkSNSSender = hybridRecordSender,
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
  )

  try {
    info(s"Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
