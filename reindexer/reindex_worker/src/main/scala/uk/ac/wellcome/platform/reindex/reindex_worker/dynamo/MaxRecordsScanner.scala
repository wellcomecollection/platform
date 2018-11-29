package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec

import scala.concurrent.Future

class MaxRecordsScanner(scanSpecScanner: ScanSpecScanner) {

  /** Run a DynamoDB Scan that returns at most `maxResults` values.
    *
    * It may return less if there aren't enough results in the table, or if
    * `maxResults` is larger than the maximum page size.
    */
  def scan(maxRecords: Int)(tableName: String): Future[List[String]] = {

    val scanSpec = new ScanSpec()
      .withMaxResultSize(maxRecords)

    scanSpecScanner.scan(scanSpec = scanSpec, tableName = tableName)
  }
}
