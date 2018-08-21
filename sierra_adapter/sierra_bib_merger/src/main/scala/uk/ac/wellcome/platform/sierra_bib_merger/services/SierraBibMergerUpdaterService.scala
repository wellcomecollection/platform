package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{HybridRecord, SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             SourceMetadata,
                                             ObjectStore[SierraTransformable]]
)(implicit ec: ExecutionContext) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[HybridRecord] =
    versionedHybridStore.updateRecord(id = bibRecord.id.withoutCheckDigit)(
      ifNotExisting = (SierraTransformable(bibRecord), SourceMetadata("sierra")))(
      ifExisting = (existingSierraTransformable, existingMetadata) => {
        (
          BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord),
          existingMetadata)
      }
    ).map{case (hybridRecord, _) => hybridRecord}
}
