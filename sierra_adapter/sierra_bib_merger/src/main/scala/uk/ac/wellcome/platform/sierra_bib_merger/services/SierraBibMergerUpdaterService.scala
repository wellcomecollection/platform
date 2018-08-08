package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             EmptyMetadata,
                                             ObjectStore[SierraTransformable]]
) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[Unit] =
    versionedHybridStore.updateRecord(id = bibRecord.id.withoutCheckDigit)(
      ifNotExisting = (SierraTransformable(bibRecord), EmptyMetadata()))(
      ifExisting = (existingSierraTransformable, existingMetadata) => {
        (
          BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord),
          existingMetadata)
      }
    )
}
