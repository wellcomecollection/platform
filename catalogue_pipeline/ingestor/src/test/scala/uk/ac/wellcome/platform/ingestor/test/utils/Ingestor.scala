package uk.ac.wellcome.platform.ingestor.test.utils

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.platform.ingestor.Server
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  IndexedElasticSearchLocal,
  SQSLocal
}

trait Ingestor
    extends IndexedElasticSearchLocal
    with SQSLocal
    with BeforeAndAfterEach
    with AmazonCloudWatchFlag { this: Suite =>

  val ingestorQueueUrl = createQueueAndReturnUrl("test_es_ingestor_queue")

  val indexName = "works"
  val itemType = "work"

  def createServer: EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> ingestorQueueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> "9200",
        "es.name" -> "wellcome",
        "es.index" -> indexName,
        "es.type" -> itemType
      ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag
    )
  }
}
