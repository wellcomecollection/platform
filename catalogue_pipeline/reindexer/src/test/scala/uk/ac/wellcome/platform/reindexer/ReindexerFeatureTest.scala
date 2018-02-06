package uk.ac.wellcome.platform.reindexer

import com.gu.scanamo.Scanamo
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, ExtendedPatience}

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with DynamoDBLocal
    with AmazonCloudWatchFlag {

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.reindexTracker.tableName" -> reindexTableName,
        "reindex.target.tableName" -> "MiroData",
        "reindex.target.reindexShard" -> "default"
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
    )

  val currentVersion = 1
  val requestedVersion = 2

  it(
    "should increment the reindexVersion to the value requested on all items of a table in need of reindex"
  ) {

    val numberOfbatches = 4
    val itemsToPut =
      generateMiroTransformablesInBatches(numberOfbatches, currentVersion)

    val expectedMiroTransformableList = itemsToPut.map(item => {
      Right(item.copy(ReindexVersion = requestedVersion))
    })

    val reindex = Reindex(miroDataTableName,
                          reindexShard,
                          requestedVersion,
                          currentVersion)
    val reindexList = List(reindex)

    itemsToPut.foreach(Scanamo.put(dynamoDbClient)(miroDataTableName))
    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    server.start()

    eventually {
      Scanamo.scan[MiroTransformable](dynamoDbClient)(miroDataTableName) should contain theSameElementsAs expectedMiroTransformableList
    }

    server.close()
  }
}
