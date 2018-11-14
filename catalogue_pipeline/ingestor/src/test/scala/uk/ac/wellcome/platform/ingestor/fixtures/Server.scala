package uk.ac.wellcome.platform.ingestor.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.platform.ingestor.{Server => AppServer}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch with Messaging with ElasticsearchFixtures {
  this: Suite =>
  def withServer[R](
    queue: Queue,
    bucket: Bucket,
    indexName: String)(testWith: TestWith[EmbeddedHttpServer, R]): R = {

    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = messageReaderLocalFlags(bucket, queue) ++ ingestEsLocalFlags(
        indexName) ++ cloudWatchLocalFlags ++ Map(
        "es.ingest.flushInterval" -> "5 seconds")
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
