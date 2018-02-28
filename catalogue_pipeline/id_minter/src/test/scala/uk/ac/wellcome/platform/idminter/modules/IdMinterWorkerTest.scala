package uk.ac.wellcome.platform.idminter.modules

import java.sql.SQLSyntaxErrorException

import org.scalatest.FunSpec
import org.scalatest.mockito.MockitoSugar
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax
import uk.ac.wellcome.platform.idminter.database.{
  FieldDescription,
  IdentifiersDao
}
import uk.ac.wellcome.platform.idminter.utils.IdMinterTestUtils

class IdMinterWorkerTest
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
      DB readOnly { implicit session =>
        sql"DESCRIBE $database.$tableName"
          .map(
            rs =>
              FieldDescription(
                rs.string("Field"),
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
              FieldDescription(
                rs.string("Field"),
                rs.string("Type"),
                rs.string("Null"),
                rs.string("Key")))
          .list()
          .apply()
      }

      fields.length should be > 0
    }
  }
}
