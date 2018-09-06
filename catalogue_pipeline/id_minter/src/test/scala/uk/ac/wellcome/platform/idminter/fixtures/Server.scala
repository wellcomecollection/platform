package uk.ac.wellcome.platform.idminter.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.platform.idminter.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.TestWith

trait Server extends CloudWatch { this: Suite =>
  def withServer[R](flags: Map[String, String])(
    testWith: TestWith[EmbeddedHttpServer, R]): R =
    withModifiedServer[R](flags = flags, modifyServer = identity)(testWith)

  def withModifiedServer[R](
    flags: Map[String, String],
    modifyServer: EmbeddedHttpServer => EmbeddedHttpServer)(
    testWith: TestWith[EmbeddedHttpServer, R]): R = {
    val server: EmbeddedHttpServer = modifyServer(
      new EmbeddedHttpServer(
        new AppServer(),
        flags = flags ++ cloudWatchLocalFlags
      ))

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
