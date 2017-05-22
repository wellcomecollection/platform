package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.query.Query
import com.gu.scanamo.query.KeyEquals
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig) {

  val tableName = dynamoConfig.table
  val gsiName = "ReindexTracker"

  def runAllIndicesReindex =
    Future.sequence(getIndicesForReindex.map(runReindex))


  def runReindex(reindex: Reindex) = {
    getRowsWithOldReindexVersion(reindex)
    // todo: more steps!
  }
  def getRowsWithOldReindexVersion(reindex: Reindex) = Future {
    val query = reindex match {
      case Reindex("MiroData", _, _) => Scanamo.queryIndex[MiroTransformable](dynamoDBClient) _
      case Reindex("CalmData", _, _) => Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
      case _ => throw new RuntimeException("nope")
    }

    //todo: This query must perform a comparison on the range key!
     query(reindex.TableName, gsiName)(
      Query(KeyEquals('ReindexShard, "default")))
  }

  def getIndicesForReindex = getIndices.filter {
    case Reindex(_, requested, current) if requested > current => true
    case _ => false
  }

  def getIndices: List[Reindex] = {
    Scanamo.scan[Reindex](dynamoDBClient)(dynamoConfig.table).map {
      case Right(reindexes) => reindexes
      case _ => throw new RuntimeException("nope")
    }
  }
}
