package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class MaxRecordsScanner @Inject()(scanSpecScanner: ScanSpecScanner, dynamoConfig: DynamoConfig) {

  /** Run a DynamoDB Scan that returns at most `maxResults` values.
    *
    * It may return less if there aren't enough results in the table, or if
    * `maxResults` is larger than the maximum page size.
    */
  def scan[T](maxRecords: Int)(implicit dynamoFormat: DynamoFormat[T])
  : Future[List[Either[DynamoReadError, T]]] = {

    val scanSpec = new ScanSpec()
      .withMaxResultSize(maxRecords)

    scanSpecScanner.scan(scanSpec = scanSpec, tableName = dynamoConfig.table)
  }
}
