package uk.ac.wellcome.platform.archive.registrar.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  VHSIndexEntry,
  VersionedHybridStore
}
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait StorageManifestVHSFixture extends LocalVersionedHybridStore {
  type StorageManifestVHS = VersionedHybridStore[StorageManifest,
                                                 EmptyMetadata,
                                                 ObjectStore[StorageManifest]]

  def withStorageManifestVHS[R](table: Table, bucket: Bucket)(
    testWith: TestWith[StorageManifestVHS, R]): R =
    withTypeVHS[StorageManifest, EmptyMetadata, R](bucket, table) { vhs =>
      testWith(vhs)
    }

  def storeSingleManifest(
    vhs: StorageManifestVHS,
    storageManifest: StorageManifest): Future[VHSIndexEntry[EmptyMetadata]] =
    vhs.updateRecord(
      id =
        s"${storageManifest.id.space}/${storageManifest.id.externalIdentifier}"
    )(
      ifNotExisting = (storageManifest, EmptyMetadata())
    )(
      ifExisting = (_, _) => throw new RuntimeException("VHS should be empty!")
    )
}
