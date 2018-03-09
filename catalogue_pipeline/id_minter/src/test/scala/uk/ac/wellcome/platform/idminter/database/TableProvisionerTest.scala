package uk.ac.wellcome.platform.idminter.database

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.platform.idminter.fixtures
import scalikejdbc._
import uk.ac.wellcome.test.utils.ExtendedPatience

case class FieldDescription(field: String,
                            dataType: String,
                            nullable: String,
                            key: String)

class TableProvisionerTest
    extends FunSpec
    with fixtures.IdentifiersDatabase
    with Eventually
    with ExtendedPatience
    with Matchers {

  it("should create the Identifiers table") {
    withIdentifiersDatabase { databaseConfig =>

      val database = databaseConfig.database
      val table = databaseConfig.table

      val databaseName = databaseConfig.databaseName
      val tableName = databaseConfig.tableName

      new TableProvisioner(host, port, username, password)
        .provision(databaseName, tableName)

      eventually {
        val fields = DB readOnly { implicit session =>
          sql"DESCRIBE $database.$table"
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

        fields.sortBy(_.field) shouldBe Seq(
          FieldDescription(
            field = "CanonicalId",
            dataType = "varchar(255)",
            nullable = "NO",
            key = "PRI"),
          FieldDescription(
            field = "OntologyType",
            dataType = "varchar(255)",
            nullable = "NO",
            key = "MUL"),
          FieldDescription(
            field = "SourceSystem",
            dataType = "varchar(255)",
            nullable = "NO",
            key = ""),
          FieldDescription(
            field = "SourceId",
            dataType = "varchar(255)",
            nullable = "NO",
            key = "")
        ).sortBy(_.field)
      }
    }
  }
}
