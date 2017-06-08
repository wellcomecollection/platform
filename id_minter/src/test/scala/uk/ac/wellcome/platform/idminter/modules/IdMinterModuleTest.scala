package uk.ac.wellcome.platform.idminter.modules

import java.sql.SQLSyntaxErrorException

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.database.{FieldDescription, IdentifiersDao}
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

  val database = SQLSyntax.createUnsafely("identifiers")
  val tableName = SQLSyntax.createUnsafely("Identifiers")

  override def beforeEach(): Unit = {
    sql"drop table if exists $database.$tableName".execute().apply()
  }

  it("should create the Identifiers table in MySQL upon startup") {
    intercept[SQLSyntaxErrorException] {
      DB readOnly { implicit session => sql"DESCRIBE $database.$tableName"
        .map(
          rs =>
            FieldDescription(rs.string("Field"),
              rs.string("Type"),
              rs.string("Null"),
              rs.string("Key")))
        .list()
        .apply()
      }
    }

    server.start()

    eventually {
      val fields = DB readOnly { implicit session =>
        sql"DESCRIBE $database.$tableName"
          .map(
            rs =>
              FieldDescription(rs.string("Field"),
                rs.string("Type"),
                rs.string("Null"),
                rs.string("Key")))
          .list()
          .apply()
      }

      fields.length should be > 0
    }
  }

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
