package uk.ac.wellcome.platform.api.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.platform.api.{Server => AppServer}
import uk.ac.wellcome.fixtures.TestWith

trait Server { this: Suite =>
  def withServer[R](flags: Map[String, String])(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      new AppServer(),
      flags = flags
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
