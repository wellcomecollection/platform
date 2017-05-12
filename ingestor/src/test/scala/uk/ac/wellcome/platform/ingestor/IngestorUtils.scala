package uk.ac.wellcome.platform.ingestor

import com.amazonaws.services.sqs.AmazonSQS
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.test.utils.{IndexedElasticSearchLocal, SQSLocal}

trait IngestorUtils extends IndexedElasticSearchLocal with SQSLocal {
  this: Suite =>
  val ingestorQueueInfo = createQueueAndDlqReturnUrls("test_es_ingestor_queue")

  def createServer: EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> ingestorQueueInfo.queueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> "9300",
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false",
        "es.index" -> indexName,
        "es.type" -> itemType
      )
    ).bind[AmazonSQS](sqsClient)
  }
}
