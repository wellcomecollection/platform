package uk.ac.wellcome.platform.reindex_request_creator.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.reindex_request_creator.{Server => AppServer}
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
