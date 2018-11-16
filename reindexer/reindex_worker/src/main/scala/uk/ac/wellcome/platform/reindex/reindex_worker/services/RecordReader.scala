package uk.ac.wellcome.platform.reindex.reindex_worker.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{MaxRecordsScanner, ParallelScanner}
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{CompleteReindexParameters, PartialReindexParameters, ReindexParameters}
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

/** Find IDs for records in the SourceData table that need reindexing.
  *
  * This class should only be doing reading -- deciding how to act on records
  * that need reindexing is the responsibility of another class.
  */
class RecordReader(
  maxRecordsScanner: MaxRecordsScanner,
  parallelScanner: ParallelScanner
) extends Logging {

  def findRecordsForReindexing(dynamoConfig: DynamoConfig, reindexJob: ReindexParameters): Future[List[String]] = {
    debug(s"Finding records that need reindexing for $reindexJob")

    val scannerMethod =
      reindexJob match {
        case CompleteReindexParameters(segment, totalSegments) =>
          parallelScanner
            .scan(
              segment = segment,
              totalSegments = totalSegments
            )
        case PartialReindexParameters(maxRecords) =>
          maxRecordsScanner.scan(maxRecords = maxRecords)
      }

    scannerMethod(dynamoConfig)
  }
}
