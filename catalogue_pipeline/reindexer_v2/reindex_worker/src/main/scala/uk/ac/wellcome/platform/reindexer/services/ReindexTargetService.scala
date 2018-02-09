package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.{UniqueKey, _}
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import com.gu.scanamo.syntax._
import com.gu.scanamo.update.UpdateExpression
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.platform.reindexer.models.{ReindexAttempt, ReindexStatus}
import uk.ac.wellcome.reindexer.models.ScanamoQueryStream
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexTargetService[T <: Reindexable[String]] @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender,
  @Flag("reindex.target.tableName") targetTableName: String,
  @Flag("reindex.target.reindexShard") targetReindexShard: String = "default")
    extends Logging {

  type ScanamoQueryResult = Either[DynamoReadError, T]

  type ScanamoQueryResultFunction =
    (List[ScanamoQueryResult]) => Boolean

  private val gsiName = "ReindexTracker"

  private def scanamoUpdate(k: UniqueKey[_],
                            updateExpression: UpdateExpression)(
    implicit evidence: DynamoFormat[T]): Either[DynamoReadError, T] =
    Scanamo.update[T](dynamoDBClient)(targetTableName)(k, updateExpression)

  private def scanamoQueryStreamFunction(queryRequest: ScanamoQueryRequest,
                                         f: ScanamoQueryResultFunction)(
    implicit evidence: DynamoFormat[T]): ScanamoOps[List[Boolean]] =
    ScanamoQueryStream.run[T, Boolean](queryRequest, f)

  private def updateVersion(requestedVersion: Int)(
    resultGroup: List[ScanamoQueryResult])(
    implicit evidence: DynamoFormat[T]): Boolean = {
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
          KeyEquals('ReindexShard, targetReindexShard),
          KeyIs('ReindexVersion, LT, requestedVersion)
        )),
      ScanamoQueryOptions.default
    )

  def runReindex(reindexAttempt: ReindexAttempt)(
    implicit evidence: DynamoFormat[T]): Future[ReindexAttempt] = {
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
