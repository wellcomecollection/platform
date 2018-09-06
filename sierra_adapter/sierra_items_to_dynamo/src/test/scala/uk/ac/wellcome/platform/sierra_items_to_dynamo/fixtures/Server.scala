package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.platform.sierra_items_to_dynamo.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch { this: Suite =>
  def withServer[R](flags: Map[String, String])(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = flags ++ cloudWatchLocalFlags
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
