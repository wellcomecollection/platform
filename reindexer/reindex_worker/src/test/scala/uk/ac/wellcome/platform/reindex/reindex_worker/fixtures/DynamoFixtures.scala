package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner,
  ScanSpecScanner
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait DynamoFixtures extends LocalDynamoDb {
  def withScanSpecScanner[R](testWith: TestWith[ScanSpecScanner, R]): R = {
    val scanner = new ScanSpecScanner(dynamoDbClient)

    testWith(scanner)
  }

  def withParallelScanner[R](table: Table)(
    testWith: TestWith[ParallelScanner, R]): R = {
    withScanSpecScanner { scanSpecScanner =>
      val scanner = new ParallelScanner(
        scanSpecScanner = scanSpecScanner,
        dynamoConfig = DynamoConfig(
          table = table.name,
          index = table.index
        )
      )

      testWith(scanner)
    }
  }

  def withMaxRecordsScanner[R](table: Table)(
    testWith: TestWith[MaxRecordsScanner, R]): R = {
    withScanSpecScanner { scanSpecScanner =>
      val scanner = new MaxRecordsScanner(
        scanSpecScanner = scanSpecScanner,
        dynamoConfig = DynamoConfig(
          table = table.name,
          index = table.index
        )
      )

      testWith(scanner)
    }
  }
}
