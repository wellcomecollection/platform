package uk.ac.wellcome.transformer.utils

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.Suite
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.test.utils._
import uk.ac.wellcome.transformer.Server

trait TransformerFeatureTest
    extends FeatureTestMixin
    with ExtendedPatience
    with Eventually
    with SNSLocal
    with SQSLocal
    with AmazonCloudWatchFlag
    with S3Local { this: Suite =>
  val flags: Map[String, String]

  lazy val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags ++ snsLocalFlags ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ s3LocalFlags
    )

  val idMinterTopicArn: String = createTopicAndReturnArn("test_id_minter")

}
