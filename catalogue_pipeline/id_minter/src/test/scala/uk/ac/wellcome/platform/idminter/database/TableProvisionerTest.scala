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

  it("creates the Identifiers table") {
    withIdentifiersDatabase { identifiersTableConfig =>
      val databaseName = identifiersTableConfig.database
      val tableName = identifiersTableConfig.tableName

      new TableProvisioner(rdsClientConfig)
        .provision(databaseName, tableName)

      eventuallyTableExists(identifiersTableConfig)
    }
  }
}
