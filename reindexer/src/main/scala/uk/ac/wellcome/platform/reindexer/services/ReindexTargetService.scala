package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import com.gu.scanamo.syntax._
import com.gu.scanamo.update.UpdateExpression
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Reindexable
import uk.ac.wellcome.platform.reindexer.models.{ReindexAttempt, ReindexStatus}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

abstract class ReindexTargetService[T <: Reindexable[String]](
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender,
  targetTableName: String)
    extends Logging {

  import uk.ac.wellcome.utils.ScanamoUtils._

  type ScanamoQueryResult = Either[DynamoReadError, T]

  type ScanamoUpdate =
    (UniqueKey[_], UpdateExpression) => Either[DynamoReadError, T]

  type ScanamoQueryResultFunction =
    (List[ScanamoQueryResult]) => List[ScanamoQueryResult]

  type ScanamoQueryStreamFunction = (
    ScanamoQueryRequest,
    ScanamoQueryResultFunction) => ScanamoOps[List[ScanamoQueryResult]]

  private val gsiName = "ReindexTracker"

  protected val scanamoUpdate: (UniqueKey[_],
                                UpdateExpression) => Either[DynamoReadError, T]

  protected val scanamoQueryStreamFunction: ScanamoQueryStreamFunction

  private def updateVersion(resultGroup: List[ScanamoQueryResult]) = {
    val updatedResults = resultGroup.map {
      case Left(e) => Left(e)
      case Right(miroTransformable) => {
        val reindexItem = miroTransformable.getReindexItem

        scanamoUpdate(reindexItem.hashKey and reindexItem.rangeKey,
                      set('ReindexVersion -> (reindexItem.ReindexVersion + 1)))
      }
    }

    if(updatedResults.length > 0) {
      ReindexStatus.progress(updatedResults.length, 1)
    }

    updatedResults
  }

  private def createScanamoQueryRequest(
    requestedVersion: Int): ScanamoQueryRequest =
    ScanamoQueryRequest(
      targetTableName,
      Some(gsiName),
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, requestedVersion)
        )),
      ScanamoQueryOptions.default
    )

  def runReindex(reindexAttempt: ReindexAttempt): Future[ReindexAttempt] = {
    info(s"ReindexTargetService running $reindexAttempt")

    val scanamoQueryRequest: ScanamoQueryRequest = createScanamoQueryRequest(
      reindexAttempt.reindex.RequestedVersion)

    val ops = scanamoQueryStreamFunction(scanamoQueryRequest, updateVersion)

    for {
      result <- Future(Scanamo.exec(dynamoDBClient)(ops))
      updatedRows = logAndFilterLeft(result)
    } yield
      reindexAttempt.copy(successful = updatedRows,
                          attempt = reindexAttempt.attempt + 1)
  }
}
