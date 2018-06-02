package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait DynamoInserterFixture extends LocalDynamoDbVersioned {

  def withDynamoInserter[R](table: Table)(
    testWith: TestWith[DynamoInserter, R]): Unit = {
    val dynamoInserter = new DynamoInserter(
      new VersionedDao(
        dynamoDbClient,
        dynamoConfig = DynamoConfig(table = table.name, index = table.index)
      )
    )

    testWith(dynamoInserter)
  }
}
