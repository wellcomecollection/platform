package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.sierra_adapter.fixtures.SierraItemRecordVHS
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait DynamoInserterFixture extends SierraItemRecordVHS {
  def withDynamoInserter[R](table: Table, bucket: Bucket)(
    testWith: TestWith[DynamoInserter, R]): Unit =
    withItemRecordVHS(table, bucket) { versionedHybridStore =>
      val dynamoInserter =
        new DynamoInserter(versionedHybridStore = versionedHybridStore)
      testWith(dynamoInserter)
    }
}
