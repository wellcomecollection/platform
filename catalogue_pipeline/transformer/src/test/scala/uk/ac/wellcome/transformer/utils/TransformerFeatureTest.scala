package uk.ac.wellcome.transformer.utils

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.Suite
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.platform.transformer.Server
import uk.ac.wellcome.test.utils._

trait TransformerFeatureTest
    extends FeatureTestMixin
    with ExtendedPatience
    with Eventually
    with SNSLocal
    with SQSLocal
    with AmazonCloudWatchFlag { this: Suite =>
  val flags: Map[String, String]

  lazy val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags ++ snsLocalEndpointFlags ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag
    )

  val idMinterTopicArn: String = createTopicAndReturnArn("test_id_minter")

}
