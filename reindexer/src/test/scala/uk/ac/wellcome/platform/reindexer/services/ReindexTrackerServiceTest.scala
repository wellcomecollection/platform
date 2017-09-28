package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}

class ReindexTrackerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience {

  val dynamoConfigs = Map(
    "reindex" -> DynamoConfig("applicationName",
                              "streamArn",
                              reindexTableName),
    "calm" -> DynamoConfig("applicationName", "streamArn", calmDataTableName))

  it(
    "should return the index in need of reindexing"
  ) {
    val expectedReindex = Reindex("CalmData", "default", 2, 1)
    val anotherReindex = Reindex("MiroData", "default", 2, 1)
    val reindexList = List(
      expectedReindex,
      anotherReindex
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexTrackerService = new ReindexTrackerService(
      dynamoDbClient,
      dynamoConfigs,
      "CalmData",
      reindexShard
    )

    val op = reindexTrackerService.getIndexForReindex

    whenReady(op) { (reindex: Option[Reindex]) =>
      Some(expectedReindex) shouldBe reindex
    }
  }
}
