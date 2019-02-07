package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner,
  ScanSpecScanner
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait DynamoFixtures extends LocalDynamoDb {
  def withScanSpecScanner[R](testWith: TestWith[ScanSpecScanner, R]): R = {
    val scanner = new ScanSpecScanner(dynamoDbClient)

    testWith(scanner)
  }

  def withParallelScanner[R](testWith: TestWith[ParallelScanner, R]): R =
    withScanSpecScanner { scanSpecScanner =>
      val scanner = new ParallelScanner(scanSpecScanner = scanSpecScanner)

      testWith(scanner)
    }

  def withMaxRecordsScanner[R](testWith: TestWith[MaxRecordsScanner, R]): R =
    withScanSpecScanner { scanSpecScanner =>
      val scanner = new MaxRecordsScanner(scanSpecScanner = scanSpecScanner)

      testWith(scanner)
    }
}
