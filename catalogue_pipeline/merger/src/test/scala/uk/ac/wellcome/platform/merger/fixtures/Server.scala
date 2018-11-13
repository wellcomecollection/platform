package uk.ac.wellcome.platform.merger.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Server
    extends CloudWatch
    with SQS
    with Messaging
    with LocalVersionedHybridStore { this: Suite =>
  def withServer[R](
    queue: Queue,
    topic: Topic,
    storageBucket: Bucket,
    messageBucket: Bucket,
    table: Table)(testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = sqsLocalFlags(queue) ++ cloudWatchLocalFlags ++ messageWriterLocalFlags(
        messageBucket,
        topic) ++ vhsLocalFlags(storageBucket, table)
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
