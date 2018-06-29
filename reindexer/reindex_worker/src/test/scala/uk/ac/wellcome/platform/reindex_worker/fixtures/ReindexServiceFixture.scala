package uk.ac.wellcome.platform.reindex_worker.fixtures

import org.scalatest.Assertion
import uk.ac.wellcome.platform.reindex_worker.services.ReindexService
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures.TestWith

trait ReindexServiceFixture extends LocalDynamoDbVersioned {
  def withReindexService(table: Table)(
    testWith: TestWith[ReindexService, Assertion]) = {
    val reindexService = new ReindexService(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = table.name,
        index = table.index
      )
    )

    testWith(reindexService)
  }
}
