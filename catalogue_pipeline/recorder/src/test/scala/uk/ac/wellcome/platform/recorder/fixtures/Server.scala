package uk.ac.wellcome.platform.recorder.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.platform.recorder.{Server => AppServer}
import uk.ac.wellcome.test.fixtures.{ServerFixtures, TestWith}

trait Server extends ServerFixtures with CloudWatch { this: Suite =>
  def withServer[R](flags: Map[String, String],modifyServer: EmbeddedHttpServer => EmbeddedHttpServer = identity)(testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer[R](new AppServer, flags ++ Map("aws.region" -> "localhost") ++ cloudWatchLocalFlags, modifyServer)(testWith)}
