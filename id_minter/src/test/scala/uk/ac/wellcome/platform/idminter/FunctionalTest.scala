package uk.ac.wellcome.platform.idminter


import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.app.TestInjector
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules.{DynamoConfigModule, SNSConfigModule, SQSConfigModule}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, Identifier, UnifiedItem}
import uk.ac.wellcome.platform.idminter.modules.{Id, IdMinterModule, SQSReaderModule}
import uk.ac.wellcome.utils.JsonUtil

class FunctionalTest extends IntegrationTestBase with Eventually with IntegrationPatience {

  override def injector =
    TestInjector(
      flags =
        Map(
          "aws.region" -> "local",
          "aws.sqs.queue.url" -> idMinterQueueUrl,
          "aws.sqs.wait.seconds" -> "1",
          "aws.sns.topic.arn" -> ingestTopicArn
        ),
      modules =
        Seq(
          LocalSNSClient,
          DynamoDBLocalClientModule,
          IdMinterModule,
          SQSReaderModule,
          SQSLocalClientModule,
          SNSConfigModule,
          SQSConfigModule,
          DynamoConfigModule))

  test("it should read a unified item from the SQS queue, generate a canonical id, save it in dynamoDB and send a message to the SNS topic with the original unified item and the id") {
    val unifiedItem = UnifiedItem("id", List(Identifier("Miro", "MiroID", "1234")), Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),UnifiedItem.json(unifiedItem), "topic", "messageType", "timestamp")

    sqsClient.sendMessage(idMinterQueueUrl, JsonUtil.toJson(sqsMessage).get)

    IdMinterModule.singletonStartup(injector)

    eventually {
      val dynamoIdentifiersRecords = Scanamo.queryIndex[Id](dynamoDbClient)("Identifiers", "MiroID")('MiroID -> "1234")
      dynamoIdentifiersRecords should have size(1)
      dynamoIdentifiersRecords.head shouldBe a[Right[DynamoReadError,Id]]
      val id = extractId(dynamoIdentifiersRecords)
      id.MiroID shouldBe "1234"

      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      JsonUtil.fromJson[IdentifiedUnifiedItem](messages.head.trim).get shouldBe IdentifiedUnifiedItem(id.CanonicalID,unifiedItem)
    }
  }

  private def extractId(dynamoIdentifiersRecords: List[Either[DynamoReadError, Id]]) = {
    val id = dynamoIdentifiersRecords.head.asInstanceOf[Right[DynamoReadError, Id]].b
    id
  }

  private def listMessagesReceivedFromSNS() = {
    val string = scala.io.Source.fromURL(localSNSEnpointUrl).mkString
    string.split('\n').filter(_.contains(":message: ")).map {_.replace(":message: ", "").replace("'","").trim}.toList
  }
}
