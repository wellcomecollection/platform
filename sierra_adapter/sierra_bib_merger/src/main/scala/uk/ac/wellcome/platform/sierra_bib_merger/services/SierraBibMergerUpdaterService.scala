package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveDecoder
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.VersionUpdater
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.storage.VersionedHybridStore
import uk.ac.wellcome.dynamo._

import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore,
  metrics: MetricsSender
) extends Logging {

  implicit val sierraTransformableUpdater =
    new VersionUpdater[SierraTransformable] {
      override def updateVersion(sierraTransformable: SierraTransformable,
                                 newVersion: Int): SierraTransformable = {
        sierraTransformable.copy(version = newVersion)
      }
    }

  def update(bibRecord: SierraBibRecord): Future[Unit] = {
    versionedHybridStore.updateRecord(SierraTransformable(bibRecord))(
      existingSierraTransformable => {
          BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord)
      }
    )
  }
}
