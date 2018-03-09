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
    with Matchers {

  it("should create the Identifiers table") {
    withIdentifiersDatabase { databaseConfig =>

      val database = databaseConfig.database
      val table = databaseConfig.table

      val databaseName = databaseConfig.databaseName
      val tableName = databaseConfig.tableName

      new TableProvisioner(host, port, username, password)
        .provision(databaseName, tableName)


      eventuallyTableExists(databaseConfig)
    }
  }
}
