package uk.ac.wellcome.platform.transformer.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.test.fixtures.{ServerFixtures, TestWith}
import uk.ac.wellcome.platform.transformer.{Server => AppServer}

trait Server extends ServerFixtures with CloudWatch { this: Suite =>
  def withServer[R](flags: Map[String, String])(
    testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer[R](
      new AppServer,
      flags ++ cloudWatchLocalFlags)(
      testWith)
}
