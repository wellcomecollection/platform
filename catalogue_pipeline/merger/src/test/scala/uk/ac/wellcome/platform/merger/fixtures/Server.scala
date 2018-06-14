package uk.ac.wellcome.platform.merger.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.platform.merger.{Server => AppServer}

trait Server extends CloudWatch with Messaging { this: Suite =>
  def withServer[R](bucket: Bucket, queue: Queue, topic: Topic)(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = messagingLocalFlags(bucket, topic, queue) ++ cloudWatchLocalFlags
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
