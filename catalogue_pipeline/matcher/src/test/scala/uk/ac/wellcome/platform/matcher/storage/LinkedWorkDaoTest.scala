package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  BatchGetItemRequest,
  PutItemRequest,
  QueryRequest
}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.WorkNode
import uk.ac.wellcome.storage.dynamo.DynamoConfig

class LinkedWorkDaoTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with MatcherFixtures {

  describe("Get from dynamo") {
    it("returns nothing if ids are not in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { linkedWorkDao =>
          whenReady(linkedWorkDao.get(Set("Not-there"))) { linkedWork =>
            linkedWork shouldBe Set.empty
          }
        }
      }
    }

    it("returns the stored linkedWorks in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { linkedWorkDao =>
          val existingNodeA = WorkNode("A", List("B"), "A+B")
          val existingNodeB = WorkNode("B", Nil, "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(existingNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(existingNodeB)

          whenReady(linkedWorkDao.get(Set("A", "B"))) { linkedWork =>
            linkedWork shouldBe Set(existingNodeA, existingNodeB)
          }
        }
      }
    }

    it("returns an error if fetching from dynamo fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("FAILED!")
        when(dynamoDbClient.batchGetItem(any[BatchGetItemRequest]))
          .thenThrow(expectedException)
        val matcherGraphDao = new LinkedWorkDao(
          dynamoDbClient,
          DynamoConfig(table.name, Some(table.index)))

        whenReady(matcherGraphDao.get(Set("A")).failed) { failedException =>
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

          whenReady(matcherGraphDao.get(Set("A")).failed) { failedException =>
            failedException shouldBe a[RuntimeException]
          }
        }
      }
    }
  }

  describe("Get by SetIds") {
    it("returns empty set if setIds are not in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          whenReady(matcherGraphDao.getBySetIds(Set("Not-there"))) {
            linkedWorks =>
              linkedWorks shouldBe Set()
          }
        }
      }
    }

    it("returns stored linkedWorks in dynamo") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          val existingNodeA = WorkNode("A", List("B"), "A+B")
          val existingNodeB = WorkNode("B", Nil, "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(existingNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(existingNodeB)

          whenReady(matcherGraphDao.getBySetIds(Set("A+B"))) { linkedWorks =>
            linkedWorks shouldBe Set(existingNodeA, existingNodeB)
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
        val linkedWordDao = new LinkedWorkDao(
          dynamoDbClient,
          DynamoConfig(table.name, Some(table.index)))

        whenReady(linkedWordDao.getBySetIds(Set("A+B")).failed) {
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

          whenReady(matcherGraphDao.getBySetIds(Set("A+B")).failed) {
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
          val work = WorkNode("A", List("B"), "A+B")
          whenReady(linkedWordDao.put(work)) { _ =>
            val savedLinkedWork =
              Scanamo.get[WorkNode](dynamoDbClient)(table.name)('workId -> "A")
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
        val linkedWordDao = new LinkedWorkDao(
          dynamoDbClient,
          DynamoConfig(table.name, Some(table.index)))

        whenReady(linkedWordDao.put(WorkNode("A", List("B"), "A+B")).failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }
  }

  it("cannot be instantiated if dynamoConfig.index is a None") {
    intercept[RuntimeException] {
      new LinkedWorkDao(dynamoDbClient, DynamoConfig("something", None))
    }
  }

}
