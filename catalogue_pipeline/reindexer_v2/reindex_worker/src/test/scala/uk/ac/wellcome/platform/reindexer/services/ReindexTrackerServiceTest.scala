package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience

class ReindexTrackerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience {

  val dynamoConfig = DynamoConfig(table = reindexTableName)

  it(
    "should return the index and shard in need of reindexing"
  ) {
    val expectedTable = "CalmData"
    val expectedShard = "new_shard"

    val anotherTable = "MiroData"
    val anotherShard = "another_shard"

    val expectedReindex = Reindex(expectedTable, expectedShard, 2, 1)
    val anotherReindex = Reindex(anotherTable, anotherShard, 2, 1)

    val reindexList = List(
      expectedReindex,
      Reindex(expectedTable, anotherShard, 2, 1),
      Reindex(anotherTable, expectedShard, 2, 1),
      Reindex(anotherTable, anotherShard, 2, 1)
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexTrackerService = new ReindexTrackerService(
      dynamoDbClient,
      dynamoConfig,
      expectedTable,
      expectedShard
    )

    val op = reindexTrackerService.getIndexForReindex

    whenReady(op) { (reindex: Option[Reindex]) =>
      Some(expectedReindex) shouldBe reindex
    }
  }
}
