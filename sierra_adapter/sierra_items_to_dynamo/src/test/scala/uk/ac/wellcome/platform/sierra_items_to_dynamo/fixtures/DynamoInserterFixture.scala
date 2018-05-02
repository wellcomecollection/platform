package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait DynamoInserterFixture extends LocalDynamoDb[SierraItemRecord] {
  override lazy val evidence: DynamoFormat[SierraItemRecord] =
    DynamoFormat[SierraItemRecord]

  def withDynamoInserter[R](
    testWith: TestWith[(Table, DynamoInserter), R]): Unit = {
    withLocalDynamoDbTable { table =>
      val dynamoInserter = new DynamoInserter(
        new VersionedDao(
          dynamoDbClient,
          dynamoConfig = DynamoConfig(table.name)
        )
      )

      testWith((table, dynamoInserter))
    }
  }
}
