package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, TestVersioned}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MaxResultScannerTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with LocalDynamoDbVersioned {

  it("reads a table with a single record") {
    withLocalDynamoDbTable { table =>
      withMaxResultScanner(table) { maxResultScanner =>
        val record =
          TestVersioned(id = "123", data = "hello world", version = 1)
        Scanamo.put(dynamoDbClient)(table.name)(record)

        val futureResult = maxResultScanner.scan[TestVersioned](maxResults = 1)

        whenReady(futureResult) { result =>
          result shouldBe List(Right(record))
        }
      }
    }
  }

  it("handles being asked for more records than are in the table") {
    withLocalDynamoDbTable { table =>
      withMaxResultScanner(table) { maxResultScanner =>
        val records = (1 to 5).map { id =>
          TestVersioned(id = id.toString, data = "Hello world", version = 1)
        }

        records.map { record =>
          Scanamo.put(dynamoDbClient)(table.name)(record)
        }

        val futureResult = maxResultScanner.scan[TestVersioned](maxResults = 10)

        whenReady(futureResult) { result =>
          result shouldBe List(records.map { Right })
        }
      }
    }
  }

  private def withMaxResultScanner[R](table: Table)(
    testWith: TestWith[MaxResultScanner, R]): R = {
    val scanner = new MaxResultScanner(
      scanSpecScanner = new ScanSpecScanner(dynamoDbClient),
      dynamoConfig = DynamoConfig(
        table = table.name,
        index = table.index
      )
    )

    testWith(scanner)
  }
}
