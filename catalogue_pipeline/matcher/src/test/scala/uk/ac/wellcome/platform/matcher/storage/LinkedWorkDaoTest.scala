package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{GetItemRequest, PutItemRequest, QueryRequest}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalLinkedWorkDynamoDb
import uk.ac.wellcome.platform.matcher.models.LinkedWork
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

class LinkedWorkDaoTest
  extends FunSpec
  with LocalLinkedWorkDynamoDb
  with Matchers
  with MockitoSugar
  with ScalaFutures {

  def withLinkedWorkDao[R](table: Table)(testWith: TestWith[LinkedWorkDao, R]): R = {
    val linkedDao = new LinkedWorkDao(dynamoDbClient, DynamoConfig(table.name, table.index))
    testWith(linkedDao)
  }

  describe("Get from dynamo") {
    it("returns nothing if matcher graph is not in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          whenReady(matcherGraphDao.get("Not-there")) {
            linkedWork =>
              linkedWork shouldBe None
          }
        }
      }
    }

    it("returns a stored linkedWork in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          val existingLinkedWork: LinkedWork = LinkedWork("A", List("B"), "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(existingLinkedWork)

          whenReady(matcherGraphDao.get("A")) {
            linkedWork =>
              linkedWork shouldBe Some(existingLinkedWork)
          }
        }
      }
    }

    it("returns an error if fetching from dynamo fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("FAILED!")
        when(dynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)
        val matcherGraphDao = new LinkedWorkDao(dynamoDbClient, DynamoConfig(table.name, table.index))

        whenReady(matcherGraphDao.get("A").failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }

    it("returns an error if Scanamo fails") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          case class BadRecord(workId: String)
          val badRecord: BadRecord = BadRecord(workId = "A")
          Scanamo.put(dynamoDbClient)(table.name)(badRecord)

          whenReady(matcherGraphDao.get("A").failed) {
            failedException =>
              failedException shouldBe a[RuntimeException]
          }
        }
      }
    }
  }

  describe("Get by SetId") {
    it("returns empty list if setId is not in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          whenReady(matcherGraphDao.getBySetId("Not-there")) {
            linkedWorks =>
              linkedWorks shouldBe List()
          }
        }
      }
    }

    it("returns a stored linkedWork in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          val existingLinkedWork: LinkedWork = LinkedWork("A", List("B"), "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(existingLinkedWork)

          whenReady(matcherGraphDao.getBySetId("A+B")) {
            linkedWorks =>
              linkedWorks shouldBe List(existingLinkedWork)
          }
        }
      }
    }

    it("returns an error if fetching from dynamo fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("FAILED")
        when(dynamoDbClient.query(any[QueryRequest]))
          .thenThrow(expectedException)
        val linkedWordDao = new LinkedWorkDao(dynamoDbClient, DynamoConfig(table.name, table.index))

        whenReady(linkedWordDao.getBySetId("A+B").failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }

    it("returns an error if Scanamo fails") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          case class BadRecord(workId: String, setId: String)
          val badRecord: BadRecord = BadRecord(workId = "A", setId = "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(badRecord)

          whenReady(matcherGraphDao.getBySetId("A+B").failed) {
            failedException =>
              failedException shouldBe a[RuntimeException]
          }
        }
      }
    }
  }

  describe("Insert into dynamo") {
    it("puts a linkedWork") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { linkedWordDao =>
          val work = LinkedWork("A", List("B"), "A+B")
          whenReady(linkedWordDao.put(work)) { _ =>
            val savedLinkedWork = Scanamo.get[LinkedWork](dynamoDbClient)(table.name)('workId -> "A")
            savedLinkedWork shouldBe Some(Right(work))
          }
        }
      }
    }

    it("returns an error if putting to dynamo fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("FAILED")
        when(dynamoDbClient.putItem(any[PutItemRequest]))
          .thenThrow(expectedException)
        val linkedWordDao = new LinkedWorkDao(dynamoDbClient, DynamoConfig(table.name, table.index))

        whenReady(linkedWordDao.put(LinkedWork("A", List("B"), "A+B")).failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }
  }

}

