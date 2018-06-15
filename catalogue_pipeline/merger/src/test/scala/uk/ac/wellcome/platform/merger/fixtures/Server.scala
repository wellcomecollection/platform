package uk.ac.wellcome.platform.merger.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.merger.{Server => AppServer}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch with SQS with SNS with LocalVersionedHybridStore { this: Suite =>
  def withServer[R](queue: Queue, topic: Topic, bucket: Bucket, table: Table)(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = sqsLocalFlags(queue) ++ cloudWatchLocalFlags ++ snsLocalFlags(topic) ++ vhsLocalFlags(bucket, table)
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
