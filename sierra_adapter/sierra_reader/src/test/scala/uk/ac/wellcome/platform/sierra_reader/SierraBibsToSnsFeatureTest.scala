package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  ExtendedPatience,
  SNSLocal,
  SQSLocal
}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

class SierraBibsToSnsFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SNSLocal
    with SQSLocal
    with AmazonCloudWatchFlag
    with Matchers
    with ExtendedPatience {
  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val topicArn = createTopicAndReturnArn("sierra-bibs-feature-test-topic")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.sns.topic.arn" -> topicArn,
      "sierra.apiUrl" -> "http://localhost:8080",
      "sierra.oauthKey" -> "key",
      "sierra.oauthSecret" -> "secret",
      "sierra.fields" -> "updatedDate,deletedDate,deleted,suppressed,author,title"
    ) ++ sqsLocalFlags ++ snsLocalFlags ++ cloudWatchLocalEndpointFlag
  )

  it("reads bibs from Sierra and writes them to SNS") {
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    eventually {
      // This comes from the wiremock recordings for Sierra API response
      listMessagesReceivedFromSNS() should have size 29
    }
  }
}
