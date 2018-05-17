package uk.ac.wellcome.platform.api.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.platform.api.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.{ServerFixtures, TestWith}

trait Server extends ServerFixtures { this: Suite =>
  def withServer[R](flags: Map[String, String])(testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer[R](new AppServer, flags)(testWith)
}
