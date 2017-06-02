package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{Reindex, ReindexItem, Reindexable}
import uk.ac.wellcome.platform.reindexer.models.{ReindexAttempt, ReindexStatus}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

abstract class ReindexTargetService[T <: Reindexable[String]](
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender)
    extends Logging {
  import uk.ac.wellcome.utils.ScanamoUtils._

  type ScanamoQuery =
    (String, String) => (Query[_]) => List[Either[DynamoReadError, T]]

  protected val transformableTable: Table[T]
  protected val scanamoQuery: ScanamoQuery

  private val gsiName = "ReindexTracker"

  def runReindex(reindexAttempt: ReindexAttempt): Future[ReindexAttempt] = {
    info(s"ReindexTargetService running $reindexAttempt")

    for {
      rows <- getRowsWithOldReindexVersion(reindexAttempt.reindex)
      filteredRows = logAndFilterLeft(rows)
      updateOps <- updateRows(reindexAttempt.reindex,
                              filteredRows.map(_.getReindexItem))
      updatedRows = logAndFilterLeft(updateOps)
    } yield
      reindexAttempt.copy(successful = updatedRows,
                          attempt = reindexAttempt.attempt + 1)

  }

  private def updateRows(reindex: Reindex, rows: List[ReindexItem[String]]) = {
    info(
      s"ReindexTargetService updating to ReindexVersion: ${reindex.RequestedVersion}")

    val ops = rows.map(reindexItem => {
      val uniqueKey = reindexItem.hashKey and reindexItem.rangeKey
      transformableTable
        .update(uniqueKey, set('ReindexVersion -> reindex.RequestedVersion))
    })

    // If no rows then updateGroups should be Nil
    val updateGroups = ops match {
      case Nil => Nil
      case _ =>  {
        // Group size is 10% of total length
        val updateGroupSize = Math.ceil(rows.length * 0.1).toInt
        ops.grouped(updateGroupSize).zipWithIndex
      }
    }

    info(
      s"ReindexTargetService updating ${rows.length} rows.")

    Future {
      updateGroups.flatMap {
        case (groupOps, i) => {
          val result = groupOps.map(Scanamo.exec(dynamoDBClient)(_))

          val updateCount = groupOps.length * (i + 1)
          val percentComplete = (updateCount.toFloat / rows.length.toFloat) * 100

          ReindexStatus.work(percentComplete)

          info(
            s"ReindexTargetService completed $updateCount updates: ${"%1.0f".format(percentComplete)}% complete")

          metricsSender.incrementCount("reindex-updated-items", updateCount.toDouble)

          result
        }
      }.toList

    }
  }

  private def getRowsWithOldReindexVersion(reindex: Reindex) = Future {
    info(
      s"ReindexTargetService querying ${reindex.TableName} for ReindexVersion: ${reindex.RequestedVersion}")

    scanamoQuery(reindex.TableName, gsiName)(
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, reindex.RequestedVersion)
        )
      ))
  }
}
