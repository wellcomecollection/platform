package uk.ac.wellcome.platform.api.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.platform.api.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.TestWith

trait Server { this: Suite =>
  def withServer[R](
    flags: Map[String, String],
    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer = identity
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        new AppServer(),
        flags = flags
      )
    )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
