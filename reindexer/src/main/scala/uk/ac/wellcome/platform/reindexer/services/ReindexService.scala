package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query.{KeyEquals, _}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class MiroReindexService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends ReindexService[MiroTransformable](dynamoDBClient,
                                              dynamoConfigs,
                                              reindexTargetTableName, "miro") {

  override val transformableTable: Table[MiroTransformable] =
    Table[MiroTransformable](reindexTargetTableName)

  override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[MiroTransformable](dynamoDBClient) _
}

class CalmReindexService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends ReindexService[CalmTransformable](dynamoDBClient,
                                              dynamoConfigs,
                                              reindexTargetTableName,
    "calm") {

  override val transformableTable: Table[CalmTransformable] =
    Table[CalmTransformable](reindexTargetTableName)

  override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
}

abstract class ReindexService[T <: Transformable with Reindexable[String]](
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  reindexTargetTableName: String,
  reindexTargetTableConfigId: String                                                       )
    extends Logging {

  type ScanamoQuery =
    (String, String) => (Query[_]) => List[Either[DynamoReadError, T]]

  val transformableTable: Table[T]
  val scanamoQuery: ScanamoQuery

  private val reindexTrackerTableConfigId = "reindex"
  private val gsiName = "ReindexTracker"

  private val reindexTrackerConfig = dynamoConfigs.getOrElse(
    reindexTrackerTableConfigId,
    throw new RuntimeException(
      s"ReindexTracker ($reindexTrackerTableConfigId) dynamo config not available!"))

  private val reindexTrackerTableName = reindexTrackerConfig.table

  private val reindexTargetConfig = dynamoConfigs.getOrElse(
    reindexTargetTableConfigId,
    throw new RuntimeException(
      s"ReindexTarget ($reindexTargetTableConfigId) dynamo config not available!"))

  private val reindexTable = Table[Reindex](reindexTrackerTableName)

  case class ReindexAttempt(reindex: Reindex,
                            successful: List[Reindexable[String]],
                            attempt: Int)

  def run =
    for {
      indices <- getIndicesForReindex
      attempt = indices.map(ReindexAttempt(_, Nil, 0))
      _ <- attempt.map(processReindexAttempt).get
      updates <- updateReindex(attempt.get)
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

  def updateReindex(reindexAttempt: ReindexAttempt) = Future {
    val updatedReindex = reindexAttempt.reindex.copy(
      currentVersion = reindexAttempt.reindex.requestedVersion)

    Scanamo.put[Reindex](dynamoDBClient)(reindexTrackerTableName)(
      updatedReindex)
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

  def logAndFilterLeft[Y](rows: List[Either[DynamoReadError, Y]]) = {
    rows.foreach {
      case Left(e: DynamoReadError) => error(e.toString)
      case _ => Unit
    }

    rows
      .filter {
        case Right(_) => true
        case Left(_) => false
      }
      .flatMap(_.right.toOption)
  }

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

  def getIndicesForReindex: Future[Option[Reindex]] =
    getIndices.map {
      case Reindex(tableName, requested, current) if requested > current =>
        Some(Reindex(tableName, requested, current))
      case _ => None
    }

  def getIndices: Future[Reindex] = Future {
    Scanamo.exec(dynamoDBClient)(
      reindexTable.get('TableName -> reindexTargetTableName)) match {
      case Some(Right(reindex)) => reindex
      case Some(Left(dynamoReadError)) =>
        throw new RuntimeException(
          s"Unable to read from $reindexTrackerTableName: $dynamoReadError")
      case None =>
        throw new RuntimeException(
          s"No table matching $reindexTargetTableName found")
    }
  }
}
