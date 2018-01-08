package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger
import uk.ac.wellcome.sierra_adapter.dynamo.SierraTransformableDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
                                               sierraTransformableDao: SierraTransformableDao,
                                               metrics: MetricsSender
) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[Unit] = {

    sierraTransformableDao
      .getRecord(bibRecord.id)
      .flatMap {
        case Some(existingSierraTransformable) =>
          val mergedRecord =
            BibMerger.mergeBibRecord(existingSierraTransformable, bibRecord)
          if (mergedRecord != existingSierraTransformable)
            sierraTransformableDao.updateRecord(mergedRecord)
          else Future.successful(())
        case None =>
          sierraTransformableDao.updateRecord(
            SierraTransformable(bibRecord)
          )
      }

  }
}
