package uk.ac.wellcome.test.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.Ports

trait ServerFixtures {
  def newAppServer: () => Ports
  val defaultFlags: Map[String, String]

  def withServer[R](flags: Map[String, String],
                    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer = identity)(testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        newAppServer(),
        flags = flags ++ defaultFlags
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
