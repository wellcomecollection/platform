package uk.ac.wellcome.test.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.Ports
import org.scalatest.concurrent.Eventually

trait ServerFixtures extends Eventually {
  def newAppServer: () => Ports
  val defaultFlags: Map[String, String]

  def withServer[R](flags: Map[String, String])(testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer = new EmbeddedHttpServer(
      newAppServer(),
      flags = flags ++ defaultFlags
    )

    server.start()

    try {
      testWith(server)
    } finally {
      eventually {
        server.close()
      }
    }
  }
}
