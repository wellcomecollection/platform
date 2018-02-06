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
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.platform.reindexer.models.ReindexJob
import uk.ac.wellcome.reindexer.models.ScanamoQueryStream
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexTargetService[T <: Reindexable] @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender,
  versionedDao: VersionedDao,
  @Flag("reindex.sourceData.tableName") targetTableName: String)
    extends Logging {

  type ScanamoQueryResult = Either[DynamoReadError, T]
  type ScanamoQueryResultFunction = (List[ScanamoQueryResult]) => Boolean

  private val gsiName = "reindexTracker"

  private def scanamoUpdate(
    key: UniqueKey[_],
    updateExpression: UpdateExpression)(implicit evidence: DynamoFormat[T]): Either[DynamoReadError, T] = {
      Scanamo.update[T](dynamoDBClient)(targetTableName)(key, updateExpression)
  }

  private def scanamoQueryStreamFunction(
    queryRequest: ScanamoQueryRequest,
    resultFunction: ScanamoQueryResultFunction
  )(implicit evidence: DynamoFormat[T]): ScanamoOps[List[Boolean]] =
    ScanamoQueryStream.run[T, Boolean](queryRequest, resultFunction)

  private def updateVersion(desiredVersion: Int)(
    resultGroup: List[ScanamoQueryResult])(
    implicit evidence: DynamoFormat[T]): Boolean = {
    val updatedResults = resultGroup.map {
      case Left(e) => Left(e)
      case Right(hybridRecord) => {
        scanamoUpdate(
          'id -> hybridRecord.id,
          set('ReindexVersion -> desiredVersion)
        )
      }
    }

    val performedUpdates = updatedResults.nonEmpty

    if (performedUpdates) {
      info(s"ReindexTargetService completed batch of ${updatedResults.length}")
      metricsSender.incrementCount(
        "reindex-updated-items",
        updatedResults.length
      )
    }

    performedUpdates
  }

  private def createScanamoQueryRequest(reindexJob: ReindexJob): ScanamoQueryRequest =
    ScanamoQueryRequest(
      targetTableName,
      Some(gsiName),
      Query(
        AndQueryCondition(
          KeyEquals('reindexShard, reindexJob.shardId),
          KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
        )),
      ScanamoQueryOptions.default
    )

  def runReindex(reindexJob: ReindexJob)(
      implicit evidence: DynamoFormat[T]): Future[Unit] = Future {

    info(s"ReindexTargetService running $reindexJob")

    val scanamoQueryRequest = createScanamoQueryRequest(reindexJob)

    val ops = scanamoQueryStreamFunction(
      queryRequest = scanamoQueryRequest,
      resultFunction = updateVersion(reindexJob.desiredVersion)
    )

    val result: Seq[Boolean] = Scanamo.exec(dynamoDBClient)(ops)

    if (result.contains(false)) {
      throw GracefulFailureException(new RuntimeException(
        "Not all records were successfully processed!"
      ))
    } else {
      info(s"Successfully processed reindex job $reindexJob")
    }
  }
}
