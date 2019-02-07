package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith

trait DynamoInserterFixture extends SierraItemRecordVHSFixture {
  def withDynamoInserter[R](table: Table, bucket: Bucket)(
    testWith: TestWith[DynamoInserter, R]): R =
    withItemRecordVHS(table = table, bucket = bucket) { versionedHybridStore =>
      val dynamoInserter = new DynamoInserter(
        versionedHybridStore = versionedHybridStore
      )
      testWith(dynamoInserter)
    }
}
