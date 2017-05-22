package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.DynamoDBLocal

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal {

  it(
    "should return a list of indexes with their current and requested versions") {
    val expectedReindexList = List(
      Reindex("foo", 2, 1),
      Reindex("bar", 1, 1)
    )

    expectedReindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = new ReindexService(
      dynamoDbClient,
      DynamoConfig("applicationName", "streamArn", reindexTableName))

    val indices = reindexService.getIndices

    expectedReindexList should contain theSameElementsAs indices
  }

  it(
    "should return a list of indexes in need of reindexing"
  ) {
    val reindexList = List(
      Reindex("foo", 2, 1),
      Reindex("bar", 1, 1)
    )

    val expectedReindexList = List(
      Reindex("foo", 2, 1)
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = new ReindexService(
      dynamoDbClient,
      DynamoConfig("applicationName", "streamArn", reindexTableName))

    val indices = reindexService.getIndicesForReindex

    expectedReindexList should contain theSameElementsAs indices
  }

  it(
    "should increment the reindexVersion to the value requested on all items of a table in need of reindex"
  ) {
    true should be(false)
  }

}
