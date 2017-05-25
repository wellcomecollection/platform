package uk.ac.wellcome.platform.reindexer.lib

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.{Reindex, ReindexItem, Reindexable, Transformable}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.platform.reindexer.services.ReindexTrackerService

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

abstract class ReindexService[T <: Transformable with Reindexable[String]](
  reindexTrackerService: ReindexTrackerService,
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  reindexTargetTableName: String,
  reindexTargetTableConfigId: String)
    extends Logging {

  import uk.ac.wellcome.utils.ScanamoUtils._

  type ScanamoQuery =
    (String, String) => (Query[_]) => List[Either[DynamoReadError, T]]

  val transformableTable: Table[T]
  val scanamoQuery: ScanamoQuery

  private val gsiName = "ReindexTracker"

  private val reindexTargetConfig = dynamoConfigs.getOrElse(
    reindexTargetTableConfigId,
    throw new RuntimeException(
      s"ReindexTarget ($reindexTargetTableConfigId) dynamo config not available!"))

  def run =
    for {
      indices <- reindexTrackerService.getIndicesForReindex
      attempt = indices.map(ReindexAttempt(_, Nil, 0))
      _ <- attempt.map(processReindexAttempt).get
      updates <- reindexTrackerService.updateReindex(attempt.get)
    } yield updates

  private def processReindexAttempt(
    reindexAttempt: ReindexAttempt): Future[ReindexAttempt] =
    reindexAttempt match {
      case ReindexAttempt(_, _, attempt) if attempt > 2 =>
        Future.failed(
          new RuntimeException(
            s"Giving up on $reindexAttempt, tried too many times.")) // Stop: give up!
      case ReindexAttempt(reindex, Nil, attempt) if attempt != 0 =>
        Future.successful(ReindexAttempt(reindex, Nil, attempt)) // Stop: done!
      case _ =>
        runReindex(reindexAttempt).flatMap(processReindexAttempt) // Carry on.
    }

  def runReindex(reindexAttempt: ReindexAttempt) =
    for {
      rows <- getRowsWithOldReindexVersion(reindexAttempt.reindex)
      filteredRows = logAndFilterLeft(rows)
      updateOps <- updateRows(reindexAttempt.reindex,
                              filteredRows.map(_.getReindexItem))
      updatedRows = logAndFilterLeft(updateOps)
    } yield
      reindexAttempt.copy(successful = updatedRows,
                          attempt = reindexAttempt.attempt + 1)

  def updateRows(reindex: Reindex, rows: List[ReindexItem[String]]) = {
    val ops = rows.map(reindexItem => {
      val uniqueKey = reindexItem.hashKey and reindexItem.rangeKey
      transformableTable
        .update(uniqueKey, set('ReindexVersion -> reindex.requestedVersion))
    })

    Future(ops.map(Scanamo.exec(dynamoDBClient)(_)))
  }

  def getRowsWithOldReindexVersion(reindex: Reindex) = Future {
    scanamoQuery(reindex.TableName, gsiName)(
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, reindex.requestedVersion)
        )
      ))
  }
}
