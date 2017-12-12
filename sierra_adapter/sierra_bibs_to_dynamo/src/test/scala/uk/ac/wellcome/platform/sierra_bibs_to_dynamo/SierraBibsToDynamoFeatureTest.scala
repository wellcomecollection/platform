package uk.ac.wellcome.platform.sierra_bibs_to_dynamo

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.locals.SierraBibsToDynamoDBLocal
import uk.ac.wellcome.models.SierraRecord
import uk.ac.wellcome.models.SierraRecord._
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  ExtendedPatience,
  SQSLocal
}
import uk.ac.wellcome.utils.JsonUtil
import com.gu.scanamo.Scanamo

class SierraBibsToDynamoFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SQSLocal
    with SierraBibsToDynamoDBLocal
    with AmazonCloudWatchFlag
    with Matchers
    with ExtendedPatience {
  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.sierraToDynamo.tableName" -> tableName,
      "sierra.apiUrl" -> "http://localhost:8080",
      "sierra.oauthKey" -> "key",
      "sierra.oauthSecret" -> "secret",
      "sierra.fields" -> "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields"
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  it("should read bibs from Sierra and add them to DynamoDB") {
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
      Scanamo.scan[SierraRecord](dynamoDbClient)(tableName) should have size 29
    }
  }
}
