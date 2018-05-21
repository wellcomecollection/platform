package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{GetItemRequest, PutItemRequest}
import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalMatcherDynamoDb
import uk.ac.wellcome.platform.matcher.models.LinkedWork
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith
import com.gu.scanamo.syntax._

class MatcherGraphDaoTest
  extends FunSpec
  with LocalMatcherDynamoDb
  with Matchers
  with MockitoSugar
  with ScalaFutures {

  def withMatcherDao[R](table: Table)(testWith: TestWith[MatcherGraphDao, R]): R = {
    val matcherGraphDao = new MatcherGraphDao(dynamoDbClient, DynamoConfig(table.name))
    testWith(matcherGraphDao)
  }

  describe("Get from dynamo") {
    it("returns nothing if matcher graph is not in dynamo") {
      withLocalDynamoDbTable { table =>
        withMatcherDao(table) { matcherGraphDao =>
          whenReady(matcherGraphDao.get("Not-there")) {
            linkedWork =>
              linkedWork shouldBe None
          }
        }
      }
    }

    it("returns a stored linkedWork in dynamo") {
      withLocalDynamoDbTable { table =>
        withMatcherDao(table) { matcherGraphDao =>
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
        val expectedException = new RuntimeException("AAAAAARGH!")
        when(dynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)
        val matcherGraphDao = new MatcherGraphDao(dynamoDbClient, DynamoConfig(table.name))

        whenReady(matcherGraphDao.get("A").failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }

    it("returns an error if Scanamo fails") {
      withLocalDynamoDbTable { table =>
        withMatcherDao(table) { matcherGraphDao =>
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

  describe("Insert into dynamo") {

    it("puts a linkedWork") {
      withLocalDynamoDbTable { table =>
        withMatcherDao(table) { matcherGraphDao =>
          val work = LinkedWork("A", List("B"), "A+B")
          whenReady(matcherGraphDao.put(work)) { _ =>
            val savedLinkedWork = Scanamo.get[LinkedWork](dynamoDbClient)(table.name)('workId -> "A")
            savedLinkedWork shouldBe Some(Right(work))
          }
        }
      }
    }

    it("returns an error if putting to dynamo fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("AAAAAARGH!")
        when(dynamoDbClient.putItem(any[PutItemRequest]))
          .thenThrow(expectedException)
        val matcherGraphDao = new MatcherGraphDao(dynamoDbClient, DynamoConfig(table.name))

        whenReady(matcherGraphDao.put(LinkedWork("A", List("B"), "A+B")).failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }
  }

}

