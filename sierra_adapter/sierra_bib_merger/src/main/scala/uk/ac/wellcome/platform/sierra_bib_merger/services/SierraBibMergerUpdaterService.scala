package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  HybridRecord,
  VersionedHybridStore
}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerUpdaterService(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             EmptyMetadata,
                                             ObjectStore[SierraTransformable]]
)(implicit ec: ExecutionContext)
    extends Logging {

  def update(bibRecord: SierraBibRecord): Future[HybridRecord] =
    versionedHybridStore
      .updateRecord(id = bibRecord.id.withoutCheckDigit)(
        ifNotExisting = (SierraTransformable(bibRecord), EmptyMetadata()))(
        ifExisting = (existingSierraTransformable, existingMetadata) => {
          (
            BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord),
            existingMetadata)
        }
      )
      .map { case (hybridRecord, _) => hybridRecord }
}
