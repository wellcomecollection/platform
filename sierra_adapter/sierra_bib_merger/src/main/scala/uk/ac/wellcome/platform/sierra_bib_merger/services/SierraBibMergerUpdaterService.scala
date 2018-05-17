package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.VersionedHybridStore
import uk.ac.wellcome.models.{SourceMetadata, Sourced}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.s3.S3TypeStore

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             S3TypeStore[SierraTransformable]],
  metrics: MetricsSender
) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[Unit] = {

    val sourceName = "sierra"

    versionedHybridStore.updateRecord(Sourced.id(sourceName, bibRecord.id))(
      SierraTransformable(bibRecord))(
      existingSierraTransformable => {
        BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord)
      }
    )(SourceMetadata(sourceName = sourceName))
  }
}
