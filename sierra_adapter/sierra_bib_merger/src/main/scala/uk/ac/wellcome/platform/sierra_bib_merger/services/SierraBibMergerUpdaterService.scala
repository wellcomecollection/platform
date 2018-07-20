package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             SourceMetadata,
                                             ObjectStore[SierraTransformable]]
) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[Unit] =
    versionedHybridStore.updateRecord(Sourced.id(bibRecord.id))(
      (SierraTransformable(bibRecord), SourceMetadata(sourceName)))(
      (existingSierraTransformable, existingMetadata) => {
        (
          BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord),
          existingMetadata)
      }
    )
}
