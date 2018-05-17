package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.sierra_items_to_dynamo.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.{ServerFixtures, TestWith}

trait Server extends ServerFixtures with CloudWatch { this: Suite =>
  def withServer[R](flags: Map[String, String])(testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer[R](new AppServer, flags ++ Map("aws.region" -> "localhost") ++ cloudWatchLocalFlags)(testWith)
}
