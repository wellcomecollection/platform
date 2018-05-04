package uk.ac.wellcome.platform.recorder.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.recorder.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch { this: Suite =>
  def withServer[R](
    flags: Map[String, String],
    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer = identity
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        new AppServer(),
        flags = Map(
          "aws.region" -> "localhost"
        ) ++ flags ++ cloudWatchLocalFlags
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
