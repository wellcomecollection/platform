package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraItemRecordVHSFixture extends LocalVersionedHybridStore {
  type SierraItemsVHS = VersionedHybridStore[SierraItemRecord,
                                             EmptyMetadata,
                                             ObjectStore[SierraItemRecord]]

  def withItemRecordVHS[R](table: Table, bucket: Bucket)(
    testWith: TestWith[SierraItemsVHS, R]): R =
    withTypeVHS[SierraItemRecord, EmptyMetadata, R](bucket, table) { vhs =>
      testWith(vhs)
    }
}
