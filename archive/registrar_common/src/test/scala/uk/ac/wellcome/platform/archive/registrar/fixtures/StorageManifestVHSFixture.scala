package uk.ac.wellcome.platform.archive.registrar.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait StorageManifestVHSFixture extends LocalVersionedHybridStore {
  type StorageManifestVHS = VersionedHybridStore[StorageManifest,
    EmptyMetadata,
    ObjectStore[StorageManifest]]

  def withStorageManifestVHS[R](table: Table, bucket: Bucket, s3Prefix: String = "")(
    testWith: TestWith[StorageManifestVHS, R]): R =
    withTypeVHS[StorageManifest, EmptyMetadata, R](bucket, table, s3Prefix) { vhs =>
      testWith(vhs)
    }
}
