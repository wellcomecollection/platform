package uk.ac.wellcome.platform.idminter.database

import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scalikejdbc.interpolation.SQLSyntax
import uk.ac.wellcome.platform.idminter.utils.MysqlLocal
import scalikejdbc._

case class FieldDescription(field: String,
                            dataType: String,
                            nullable: String,
                            key: String)

class TableProvisionerTest
    extends FunSpec
    with MysqlLocal
    with Matchers
    with BeforeAndAfterEach {

  val tableName: SQLSyntax = SQLSyntax.createUnsafely("Identifiers")

  override def beforeEach(): Unit = {
    super.beforeEach()
    sql"drop table if exists $identifiersDatabase.schema_version"
      .execute()
      .apply()
    sql"drop table if exists $identifiersDatabase.$tableName".execute().apply()
  }

  it("should create the Identifiers table") {
    new TableProvisioner(host, port, userName, password)
      .provision(identifiersDatabase, tableName)

    eventually {
      val fields = DB readOnly { implicit session =>
        sql"DESCRIBE $identifiersDatabase.$tableName"
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
