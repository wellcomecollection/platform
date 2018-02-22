package uk.ac.wellcome.platform.idminter.utils

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.Server
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

trait IdMinterTestUtils
    extends SQSLocal
    with SNSLocal
    with MysqlLocal
    with IdentifiersTableInfo
    with AmazonCloudWatchFlag
    with Matchers { this: Suite =>
  val ingestorTopicArn: String = createTopicAndReturnArn("test_ingestor")
  val idMinterQueue: String = createQueueAndReturnUrl("test_id_minter")

  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(idMinterQueue, Map("VisibilityTimeout" -> "1"))

  def defineServer: EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.region" -> "localhost",
        "aws.sqs.queue.url" -> idMinterQueue,
        "aws.sqs.waitTime" -> "1",
        "aws.sns.topic.arn" -> ingestorTopicArn
      ) ++ snsLocalFlags ++ sqsLocalFlags ++ identifiersMySqlLocalFlags ++ cloudWatchLocalEndpointFlag
    )
  }

  def generateSqsMessage(MiroID: String): SQSMessage = {
    val identifier =
      SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID)

    val work = UnidentifiedWork(title = Some("A query about a queue of quails"),
                    sourceIdentifier = identifier,
                    version = 1,
                    identifiers = List(identifier))

    SQSMessage(Some("subject"),
               JsonUtil.toJson(work).get,
               "topic",
               "messageType",
               "timestamp")
  }

  def assertMessageIsNotDeleted(): Unit = {
    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    eventually {
      sqsClient
        .getQueueAttributes(
          idMinterQueue,
          List("ApproximateNumberOfMessagesNotVisible")
        )
        .getAttributes
        .get("ApproximateNumberOfMessagesNotVisible") shouldBe "1"
    }
  }
}
