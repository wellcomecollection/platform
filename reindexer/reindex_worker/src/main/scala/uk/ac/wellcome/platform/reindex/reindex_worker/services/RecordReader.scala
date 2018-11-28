package uk.ac.wellcome.platform.reindex.reindex_worker.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner
}
import uk.ac.wellcome.platform.reindex.reindex_worker.models._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class RecordReader(
  maxRecordsScanner: MaxRecordsScanner,
  parallelScanner: ParallelScanner
) extends Logging {

  def findRecordsForReindexing(
    reindexParameters: ReindexParameters,
    dynamoConfig: DynamoConfig): Future[List[String]] = {
    debug(s"Finding records that need reindexing for $reindexParameters")

    reindexParameters match {
      case CompleteReindexParameters(segment, totalSegments) =>
        parallelScanner
          .scan(
            segment = segment,
            totalSegments = totalSegments
          )(tableName = dynamoConfig.table)
      case PartialReindexParameters(maxRecords) =>
        maxRecordsScanner.scan(maxRecords = maxRecords)(
          tableName = dynamoConfig.table)
    }
  }
}
