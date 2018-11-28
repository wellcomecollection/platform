package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.DynamoFixtures
import uk.ac.wellcome.storage.dynamo.TestVersioned
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParallelScannerTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with DynamoFixtures
    with LocalDynamoDbVersioned {

  it("reads a table with a single record") {
    withLocalDynamoDbTable { table =>
      withParallelScanner { parallelScanner =>
        val record =
          TestVersioned(id = "123", data = "hello world", version = 1)
        Scanamo.put(dynamoDbClient)(table.name)(record)

        val futureResult = parallelScanner.scan(
          segment = 0,
          totalSegments = 1
        )(table.name)

        whenReady(futureResult) { result =>
          result.map { fromJson[TestVersioned](_).get } shouldBe List(record)
        }
      }
    }
  }

  it("reads all the records from a table across multiple scans") {
    runTest(totalRecords = 1000, segmentCount = 6)
  }

  it("reads all the records even when segmentCount > totalRecords") {
    runTest(totalRecords = 5, segmentCount = 10)
  }

  it(
    "returns a failed future if asked for a segment that's greater than totalSegments") {
    withLocalDynamoDbTable { table =>
      withParallelScanner { parallelScanner =>
        val future = parallelScanner.scan(
          segment = 10,
          totalSegments = 5
        )(table.name)

        whenReady(future.failed) { r =>
          r shouldBe a[AmazonDynamoDBException]
          val message = r.asInstanceOf[AmazonDynamoDBException].getMessage
          message should include(
            "Value '10' at 'segment' failed to satisfy constraint: Member must have value less than or equal to 4")
        }
      }
    }
  }

  private def runTest(totalRecords: Int, segmentCount: Int): Assertion = {
    withLocalDynamoDbTable { table =>
      withParallelScanner { parallelScanner =>
        val records = (1 to totalRecords).map { id =>
          TestVersioned(id = id.toString, data = "Hello world", version = 1)
        }

        records.map { record =>
          Scanamo.put(dynamoDbClient)(table.name)(record)
        }

        // Note that segments are 0-indexed
        val futureResults = (0 until segmentCount).map { segment =>
          parallelScanner.scan(
            segment = segment,
            totalSegments = segmentCount
          )(table.name)
        }

        whenReady(Future.sequence(futureResults)) {
          actualRecords: Seq[List[String]] =>
            actualRecords.flatten.map { fromJson[TestVersioned](_).get } should contain theSameElementsAs records
        }
      }
    }
  }
}
