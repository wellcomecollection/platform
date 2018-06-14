package uk.ac.wellcome.platform.merger.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.merger.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch with SQS { this: Suite =>
  def withServer[R](queue: Queue)(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = sqsLocalFlags(queue) ++ cloudWatchLocalFlags
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
