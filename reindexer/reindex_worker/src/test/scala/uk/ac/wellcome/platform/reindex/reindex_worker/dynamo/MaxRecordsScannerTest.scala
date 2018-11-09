package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import java.util

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.DynamoFixtures
import uk.ac.wellcome.storage.dynamo.TestVersioned
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned

import scala.concurrent.Future

class MaxRecordsScannerTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with DynamoFixtures
    with LocalDynamoDbVersioned
    with ScanSpecScannerTestBase {

  it("reads a table with a single record") {
    withLocalDynamoDbTable { table =>
      withMaxRecordsScanner(table) { maxResultScanner =>
        val record =
          TestVersioned(id = "123", data = "hello world", version = 1)
        Scanamo.put(dynamoDbClient)(table.name)(record)

        val expectedRecords = List(toAttributeMap(record))

        val futureResult: Future[List[util.Map[String, AttributeValue]]] =
          maxResultScanner.scan(maxRecords = 1)

        whenReady(futureResult) { result =>
          result shouldBe expectedRecords
        }
      }
    }
  }

  it("handles being asked for more records than are in the table") {
    withLocalDynamoDbTable { table =>
      withMaxRecordsScanner(table) { maxResultScanner =>
        val records = (1 to 5).map { id =>
          TestVersioned(id = id.toString, data = "Hello world", version = 1)
        }

        records.map { record =>
          Scanamo.put(dynamoDbClient)(table.name)(record)
        }

        val futureResult = maxResultScanner.scan(maxRecords = 10)

        whenReady(futureResult) { result =>
          result should contain theSameElementsAs records.map { toAttributeMap }
        }
      }
    }
  }
}
