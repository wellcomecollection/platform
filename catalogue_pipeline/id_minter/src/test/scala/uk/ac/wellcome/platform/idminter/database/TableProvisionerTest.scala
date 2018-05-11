package uk.ac.wellcome.platform.idminter.database

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.idminter.fixtures

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
      val databaseName = databaseConfig.databaseName
      val tableName = databaseConfig.tableName

      new TableProvisioner(host, port, username, password)
        .provision(databaseName, tableName)

      eventuallyTableExists(databaseConfig)
    }
  }
}
