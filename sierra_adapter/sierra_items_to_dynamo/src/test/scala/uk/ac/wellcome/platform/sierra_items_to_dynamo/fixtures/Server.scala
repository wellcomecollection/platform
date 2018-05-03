package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.AmazonCloudWatchFlag
import uk.ac.wellcome.platform.sierra_items_to_dynamo.{Server => AppServer}

trait Server extends AmazonCloudWatchFlag { this: Suite =>
  def withServer[R](
    flags: Map[String, String],
    modifyServer: (EmbeddedHttpServer) => EmbeddedHttpServer = identity
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        new AppServer(),
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
