package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{BatchGetItemRequest, PutItemRequest, QueryRequest}
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
          whenReady(linkedWorkDao.get(Set("Not-there"))) { workNodeSet =>
            workNodeSet shouldBe Set.empty
          }
        }
      }
    }

    it("returns WorkNodes which are stored in DynamoDB") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { linkedWorkDao =>
          val workNodeA = WorkNode("A", List("B"), "A+B")
          val workNodeB = WorkNode("B", Nil, "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(linkedWorkDao.get(Set("A", "B"))) { workNodeSet =>
            workNodeSet shouldBe Set(workNodeA, workNodeB)
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
            workNodeSet => workNodeSet shouldBe Set()
          }
        }
      }
    }

    it("returns WorkNodes which are stored in DynamoDB") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { matcherGraphDao =>
          val workNodeA = WorkNode("A", List("B"), "A+B")
          val workNodeB = WorkNode("B", Nil, "A+B")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(matcherGraphDao.getBySetIds(Set("A+B"))) { workNodeSet =>
            workNodeSet shouldBe Set(workNodeA, workNodeB)
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
    it("puts a WorkNode") {
      withLocalDynamoDbTable { table =>
        withLinkedWorkDao(table) { linkedWordDao =>
          val workNode = WorkNode("A", List("B"), "A+B")
          whenReady(linkedWordDao.put(workNode)) { _ =>
            val savedWorkNode = Scanamo.get[WorkNode](dynamoDbClient)(
              table.name)('id -> "A")
            savedWorkNode shouldBe Some(Right(workNode))
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
