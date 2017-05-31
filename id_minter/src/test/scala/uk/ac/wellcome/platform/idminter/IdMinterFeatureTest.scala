package uk.ac.wellcome.platform.idminter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedWork, Identifier, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.{DynamoDBLocal, SNSLocal, SQSLocal, TestFlagsProvider}
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class IdMinterFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SQSLocal
    with SNSLocal
    with Eventually with DynamoDBLocal {

  val ingestorTopicArn: String = createTopicAndReturnArn("test_ingestor")
  val idMinterQueue: String = createQueueAndReturnUrl("test_id_minter")

  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(idMinterQueue, Map("VisibilityTimeout" -> "1"))
  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.region" -> "local",
      "aws.sqs.queue.url" -> idMinterQueue,
      "aws.sqs.waitTime" -> "1",
      "aws.sns.topic.arn" -> ingestorTopicArn,
      "aws.dynamo.identifiers.tableName" -> identifiersTableName
    ) ++ testFlags
  ).bind[AmazonSQS](sqsClient)
    .bind[AmazonSNS](amazonSNS)

  it("should read a work from the SQS queue, generate a canonical ID, save it in dynamoDB and send a message to the SNS topic with the original work and the id") {
    val miroID = "M0001234"
    val label = "A limerick about a lion"

    val work = Work(
      identifiers = List(SourceIdentifier("Miro", "MiroID", miroID)),
      label = label)
    val sqsMessage = SQSMessage(Some("subject"),
                                JsonUtil.toJson(work).get,
                                "topic",
                                "messageType",
                                "timestamp")

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)

    eventually {
      val dynamoIdentifiersRecords =
        Scanamo.queryIndex[Identifier](dynamoDbClient)(
          "Identifiers",
          "MiroID")('MiroID -> miroID)
      dynamoIdentifiersRecords should have size (1)
      val id = extractId(dynamoIdentifiersRecords)
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)

      val parsedIdentifiedWork = JsonUtil
        .fromJson[IdentifiedWork](messages.head.message)
        .get

      parsedIdentifiedWork.canonicalId shouldBe id.CanonicalID
      parsedIdentifiedWork.work.identifiers.head.value shouldBe miroID
      parsedIdentifiedWork.work.label shouldBe label

      messages.head.subject should be("identified-item")
    }
  }

  it("should keep polling the SQS queue for new messages") {
    val firstMiroId = "1234"
    val sqsMessage = generateSqsMessage(firstMiroId)

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)

    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> firstMiroId) should have size (1)
    }

    val secondMiroId = "5678"
    val secondSqsMessage = generateSqsMessage(secondMiroId)
    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(secondSqsMessage).get)

    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> secondMiroId) should have size (1)
      Scanamo.scan[Identifier](dynamoDbClient)("Identifiers") should have size (2)
    }
  }

  it("should keep polling if something fails processing a message") {
    sqsClient.sendMessage(idMinterQueue, "not a json string")

    val miroId = "1234"
    val sqsMessage = generateSqsMessage(miroId)

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)
    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> miroId) should have size (1)
    }

  }


  it("should not delete a message from the sqs queue if it fails processing it") {
    sqsClient.sendMessage(idMinterQueue, "not a json string")

    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    eventually {
      sqsClient
        .getQueueAttributes(idMinterQueue,
          List("ApproximateNumberOfMessagesNotVisible"))
        .getAttributes
        .get("ApproximateNumberOfMessagesNotVisible") shouldBe "1"
    }
  }

  private def generateSqsMessage(MiroID: String) = {
    val work = Work(identifiers =
                      List(SourceIdentifier("Miro", "MiroID", MiroID)),
                    label = "some label")
    SQSMessage(Some("subject"),
               JsonUtil.toJson(work).get,
               "topic",
               "messageType",
               "timestamp")
  }

  private def extractId(
    dynamoIdentifiersRecords: List[Either[DynamoReadError, Identifier]]) = {
    dynamoIdentifiersRecords.head
      .asInstanceOf[Right[DynamoReadError, Identifier]]
      .b
  }
}
