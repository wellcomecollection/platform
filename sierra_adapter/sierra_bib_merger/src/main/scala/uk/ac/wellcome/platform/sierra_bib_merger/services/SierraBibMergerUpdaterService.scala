package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.MergedSierraRecord
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.platform.sierra_adapter.dynamo.MergedSierraRecordDao
import uk.ac.wellcome.platform.sierra_bib_merger.merger.BibMerger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  mergedSierraRecordDao: MergedSierraRecordDao,
  metrics: MetricsSender
) extends Logging {

  def update(bibRecord: SierraBibRecord): Future[Unit] = {

    mergedSierraRecordDao
      .getRecord(bibRecord.id)
      .flatMap {
        case Some(existingMergedSierraRecord) =>
          val mergedRecord =
            BibMerger.mergeBibRecord(existingMergedSierraRecord, bibRecord)
          if (mergedRecord != existingMergedSierraRecord)
            mergedSierraRecordDao.updateRecord(mergedRecord)
          else Future.successful(())
        case None =>
          mergedSierraRecordDao.updateRecord(
            MergedSierraRecord(bibRecord)
          )
      }

  }
}
