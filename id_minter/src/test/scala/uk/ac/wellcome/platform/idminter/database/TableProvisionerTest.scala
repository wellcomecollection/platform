package uk.ac.wellcome.platform.idminter.database

import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scalikejdbc.interpolation.SQLSyntax
import uk.ac.wellcome.platform.idminter.utils.MysqlLocal
import scalikejdbc._

case class FieldDescription(field: String,
                            dataType: String,
                            nullable: String,
                            key: String)

class TableProvisionerTest extends FunSpec with MysqlLocal with Matchers with BeforeAndAfterEach{

  override def beforeEach(): Unit = {
    sql"drop table if exists identifiers.schema_version".execute().apply()
  }

  it("should create the Identifiers table") {
    val database = SQLSyntax.createUnsafely("identifiers")
    val tableName = SQLSyntax.createUnsafely("Identifiers")
    new TableProvisioner(host, port, userName, password).provision(database, tableName)

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

      fields shouldBe Seq(
        FieldDescription(field = "CanonicalID",
                         dataType = "varchar(255)",
                         nullable = "NO",
                         key = "PRI"),
        FieldDescription(field = "MiroID",
                         dataType = "varchar(255)",
                         nullable = "YES",
                         key = "UNI")
      )
    }
  }
}
