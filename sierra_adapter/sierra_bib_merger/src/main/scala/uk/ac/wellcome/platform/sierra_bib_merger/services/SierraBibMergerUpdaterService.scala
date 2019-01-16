package uk.ac.wellcome.platform.sierra_bib_merger.services

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  VHSIndexEntry,
  VersionedHybridStore
}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.Future

class SierraBibMergerUpdaterService(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             EmptyMetadata,
                                             ObjectStore[SierraTransformable]]
) {

  def update(bibRecord: SierraBibRecord): Future[VHSIndexEntry[EmptyMetadata]] =
    versionedHybridStore
      .updateRecord(id = bibRecord.id.withoutCheckDigit)(
        ifNotExisting = (SierraTransformable(bibRecord), EmptyMetadata()))(
        ifExisting = (existingSierraTransformable, existingMetadata) => {
          (
            BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord),
            existingMetadata)
        }
      )
}
