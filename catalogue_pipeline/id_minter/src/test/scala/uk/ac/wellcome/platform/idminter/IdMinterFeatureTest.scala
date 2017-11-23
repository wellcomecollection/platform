package uk.ac.wellcome.platform.idminter

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import scalikejdbc.{select, _}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdMinterTestUtils
import uk.ac.wellcome.utils.JsonUtil

class IdMinterFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with IdMinterTestUtils {

  override val server: EmbeddedHttpServer = defineServer
  private val i = identifiersTable.i

  override def beforeEach(): Unit = {
    super.beforeEach()
    sql"TRUNCATE TABLE $identifiersDatabase.$identifiersTableName"
      .execute()
      .apply()
  }

  it(
    "should read a work from the SQS queue, generate a canonical ID, save it in SQL and send a message to the SNS topic with the original work and the id") {
    val miroID = "M0001234"
    val title = "A limerick about a lion"

    val work = Work(identifiers =
                      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, miroID)),
                    title = title)

    val sqsMessage = SQSMessage(Some("subject"),
                                JsonUtil.toJson(work).get,
                                "topic",
                                "messageType",
                                "timestamp")

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)

    eventually {
      val maybeIdentifier = withSQL {
        select.from(identifiersTable as i).where.eq(i.MiroID, miroID)
      }.map(Identifier(i)).single.apply()

      maybeIdentifier shouldBe defined
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)

      val parsedIdentifiedWork = JsonUtil
        .fromJson[Work](messages.head.message)
        .get

      parsedIdentifiedWork.id shouldBe maybeIdentifier.get.CanonicalID
      parsedIdentifiedWork.identifiers.head.value shouldBe miroID
      parsedIdentifiedWork.title shouldBe title

      messages.head.subject should be("identified-item")
    }
  }

  it("should keep polling the SQS queue for new messages") {
    val firstMiroId = "1234"
    val sqsMessage = generateSqsMessage(firstMiroId)

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)

    eventually {
      withSQL {
        select.from(identifiersTable as i).where.eq(i.MiroID, firstMiroId)
      }.map(Identifier(i)).single.apply() shouldBe defined
    }

    val secondMiroId = "5678"
    val secondSqsMessage = generateSqsMessage(secondMiroId)
    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(secondSqsMessage).get)

    eventually {
      withSQL {
        select.from(identifiersTable as i).where.eq(i.MiroID, secondMiroId)
      }.map(Identifier(i)).single.apply() shouldBe defined
      withSQL {
        select.from(identifiersTable as i)
      }.map(Identifier(i)).list.apply() should have size (2)
    }
  }

  it("should keep polling if something fails processing a message") {
    sqsClient.sendMessage(idMinterQueue, "not a json string")

    val miroId = "1234"
    val sqsMessage = generateSqsMessage(miroId)

    sqsClient.sendMessage(idMinterQueue, JsonUtil.toJson(sqsMessage).get)
    eventually {
      withSQL {
        select.from(identifiersTable as i).where.eq(i.MiroID, miroId)
      }.map(Identifier(i)).single.apply() shouldBe defined
    }
  }

  it(
    "should not delete a message from the sqs queue if it fails processing it") {
    sqsClient.sendMessage(idMinterQueue, "not a json string")

    assertMessageIsNotDeleted()
  }
}
