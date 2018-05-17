package uk.ac.wellcome.test.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.Ports
import org.scalatest.Suite

trait ServerFixtures { this: Suite =>

  def withServer[R](appServer: => Ports,
                    flags: Map[String, String],
                    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer =
                      identity)(testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        appServer,
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
