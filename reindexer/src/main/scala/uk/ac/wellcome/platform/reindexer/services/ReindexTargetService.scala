package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import uk.ac.wellcome.models.{Reindex, ReindexItem, Reindexable, Transformable}
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

abstract class ReindexTargetService[T <: Transformable with Reindexable[
  String]](dynamoDBClient: AmazonDynamoDB) {
  import uk.ac.wellcome.utils.ScanamoUtils._

  type ScanamoQuery =
    (String, String) => (Query[_]) => List[Either[DynamoReadError, T]]

  val transformableTable: Table[T]
  val scanamoQuery: ScanamoQuery

  private val gsiName = "ReindexTracker"

  def runReindex(reindexAttempt: ReindexAttempt): Future[ReindexAttempt] =
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
