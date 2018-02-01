package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.utils._
import uk.ac.wellcome.utils.JsonUtil._

class SierraReaderFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with S3Local
    with SQSLocal
    with AmazonCloudWatchFlag
    with Matchers
    with ExtendedPatience {
  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  override lazy val bucketName = "sierra-reader-feature-test-bucket"

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.s3.bucketName" -> bucketName,
      "sierra.apiUrl" -> "http://localhost:8080",
      "sierra.oauthKey" -> "key",
      "sierra.oauthSecret" -> "secret",
      "sierra.fields" -> "updatedDate,deletedDate,deleted,suppressed,author,title",
      "reader.resourceType" -> "bibs"
    ) ++ sqsLocalFlags ++ s3LocalFlags ++ cloudWatchLocalEndpointFlag
  )

  it("reads bibs from Sierra and writes files to S3") {
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    eventually {
      // This comes from the wiremock recordings for Sierra API response
      s3Client.listObjects(bucketName).getObjectSummaries should have size 2
    }
  }
}
