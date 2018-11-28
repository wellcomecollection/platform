package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.DynamoInserter
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait DynamoInserterFixture extends LocalVersionedHybridStore {

  def withDynamoInserter[R](table: Table, bucket: Bucket)(
    testWith: TestWith[DynamoInserter, R]): R =
    withTypeVHS[SierraItemRecord, EmptyMetadata, R](bucket, table) {
      versionedHybridStore =>
        val dynamoInserter =
          new DynamoInserter(versionedHybridStore = versionedHybridStore)
        testWith(dynamoInserter)
    }
}
