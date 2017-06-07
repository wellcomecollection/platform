package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.model.Message
import com.twitter.finatra.http.EmbeddedHttpServer
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.Server
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.MysqlLocal
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.{SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdMinterModuleTest
    extends FunSpec
    with SNSLocal
    with MysqlLocal
    with SQSLocal
    with MockitoSugar
    with ScalaFutures with Matchers {

  private val sQSReader = mock[SQSReader]
  private val identifiersDao = mock[IdentifiersDao]
  val server = new EmbeddedHttpServer(
    new Server(),
    flags = snsLocalEndpointFlags ++ mySqlLocalEndpointFlags ++ sqsLocalFlags
  ).bind[SQSReader](sQSReader)
    .bind[IdentifiersDao](identifiersDao)

  it("should send a function that returns a failed future to sqsReader if inserting an identifier into the database fails") {
    val functionCaptor =
      ArgumentCaptor.forClass(classOf[Function[Message, Future[Unit]]])

    val miroId = "1234"
    when(sQSReader.retrieveAndDeleteMessages(functionCaptor.capture()))
      .thenReturn(Future.successful(()))
    when(identifiersDao.findSourceIdInDb(miroId))
      .thenReturn(Future.successful(None))
    val expectedException = new Exception("cannot insert")
    when(identifiersDao.saveIdentifier(any[Identifier]))
      .thenReturn(Future.failed(expectedException))

    server.start()

    eventually {

      val message = new Message().withBody(JsonUtil.toJson(SQSMessage(Some("subject"),
        JsonUtil.toJson(Work(
          identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)),
          label = "some label"
        )).get,
        "topic",
        "messageType",
        "timestamp")).get)

      val function = functionCaptor.getValue

      whenReady(function(message).failed) { exception =>
        exception shouldBe expectedException
      }
    }

    server.close()
  }
}
