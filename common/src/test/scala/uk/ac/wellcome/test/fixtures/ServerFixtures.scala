package uk.ac.wellcome.test.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.Ports
import org.scalatest.Suite
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.AmazonCloudWatchFlag

trait ServerFixtures[ServerType <: Ports] extends AmazonCloudWatchFlag { this: Suite =>
  def withServer[R](
    flags: Map[String, String],
    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer = identity
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        new ServerType(),
        flags = Map(
          "aws.region" -> "localhost"
        ) ++ flags ++ cloudWatchLocalEndpointFlag
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
