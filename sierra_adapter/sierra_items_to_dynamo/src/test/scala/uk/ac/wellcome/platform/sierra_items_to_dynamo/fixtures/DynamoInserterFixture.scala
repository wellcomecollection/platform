package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, TestWith}
import uk.ac.wellcome.test.fixtures.LocalDynamoDb.Table

import uk.ac.wellcome.dynamo._

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
