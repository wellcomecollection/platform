package uk.ac.wellcome.platform.idminter.modules

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdMinterTestUtils
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdMinterModuleTest
    extends FunSpec
    with IdMinterTestUtils
    with MockitoSugar {

  private val identifiersDao = mock[IdentifiersDao]
  private val server = defineServer.bind[IdentifiersDao](identifiersDao)

  it("should send a function that returns a failed future to sqsReader if inserting an identifier into the database fails") {
    val miroId = "1234"
    when(identifiersDao.findSourceIdInDb(miroId))
      .thenReturn(Future.successful(None))
    when(identifiersDao.saveIdentifier(any[Identifier]))
      .thenReturn(Future.failed(new Exception("cannot insert")))

    val message = JsonUtil.toJson(SQSMessage(Some("subject"),
      JsonUtil.toJson(Work(
        identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)),
        label = "some label"
      )).get,
      "topic",
      "messageType",
      "timestamp")).get

    sqsClient.sendMessage(idMinterQueue, message)

    server.start()

    assertMessageIsNotDeleted()

    server.close()
  }
}
