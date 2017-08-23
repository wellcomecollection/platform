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
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.platform.reindexer.models.{ReindexAttempt, ReindexStatus}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

abstract class ReindexTargetService[T <: Reindexable[String]](
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender,
  targetTableName: String)
    extends Logging {

  type ScanamoQueryResult = Either[DynamoReadError, T]

  type ScanamoUpdate =
    (UniqueKey[_], UpdateExpression) => Either[DynamoReadError, T]

  type ScanamoQueryResultFunction =
    (List[ScanamoQueryResult]) => Boolean

  type ScanamoQueryStreamFunction = (
    ScanamoQueryRequest,
    ScanamoQueryResultFunction) => ScanamoOps[List[Boolean]]

  private val gsiName = "ReindexTracker"

  protected val scanamoUpdate: (UniqueKey[_],
                                UpdateExpression) => Either[DynamoReadError, T]

  protected val scanamoQueryStreamFunction: ScanamoQueryStreamFunction

  private def updateVersion(requestedVersion: Int)(
    resultGroup: List[ScanamoQueryResult]): Boolean = {
    val updatedResults = resultGroup.map {
      case Left(e) => Left(e)
      case Right(miroTransformable) => {
        val reindexItem = Reindexable.getReindexItem(miroTransformable)

        scanamoUpdate(reindexItem.hashKey and reindexItem.rangeKey,
                      set('ReindexVersion -> requestedVersion))
      }
    }

    val performedUpdates = updatedResults.nonEmpty

    if (performedUpdates) {
      info(s"ReindexTargetService completed batch of ${updatedResults.length}")
      metricsSender.incrementCount("reindex-updated-items",
                                   updatedResults.length)
      ReindexStatus.progress(updatedResults.length, 1)
    }

    performedUpdates
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
    val requestedVersion = reindexAttempt.reindex.RequestedVersion

    info(s"ReindexTargetService running $reindexAttempt")

    val scanamoQueryRequest: ScanamoQueryRequest =
      createScanamoQueryRequest(requestedVersion)

    val ops = scanamoQueryStreamFunction(scanamoQueryRequest,
                                         updateVersion(requestedVersion))

    Future(Scanamo.exec(dynamoDBClient)(ops)).map(r => {
      reindexAttempt.copy(successful = !r.contains(false),
                          attempt = reindexAttempt.attempt + 1)
    })
  }
}
