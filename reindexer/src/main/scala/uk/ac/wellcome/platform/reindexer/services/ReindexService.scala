package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query.{KeyEquals, _}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               dynamoConfig: Map[String, DynamoConfig])
    extends Logging {

  val tableName = dynamoConfig
    .get("reindex")
    .getOrElse(
      throw new RuntimeException("reindex dynamo config not available"))
    .table

  val gsiName = "ReindexTracker"

  lazy val miroTable = Table[MiroTransformable]("MiroData")
  lazy val calmTable = Table[CalmTransformable]("CalmData")

  case class ReindexAttempt(reindex: Reindex,
                            successful: List[Reindexable[String]],
                            attempt: Int)

  def run =
    (for {
      indices <- getIndicesForReindex
      attempts = indices.map(ReindexAttempt(_, Nil, 0))
      completions <- Future.sequence(attempts.map(processReindexAttempt))
      updates <- Future.sequence(completions.map(updateReindex))
    } yield updates).recover {
      case e => error("Some reindexes failed to complete.", e)
    }

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

    Scanamo.put[Reindex](dynamoDBClient)(tableName)(updatedReindex)
  }

  def runReindex(reindexAttempt: ReindexAttempt) =
    for {
      rows <- getRowsWithOldReindexVersion(reindexAttempt.reindex)
      filteredRows <- Future.successful(
        logAndFilterLeft[Reindexable[String]](rows))
      updateOps <- updateRows(reindexAttempt.reindex,
                              filteredRows.map(_.getReindexItem))
      updatedRows <- Future.successful(
        logAndFilterLeft[Reindexable[String]](updateOps))
    } yield
      reindexAttempt.copy(successful = updatedRows,
                          attempt = reindexAttempt.attempt + 1)

  def logAndFilterLeft[T](rows: List[Either[DynamoReadError, T]]) = {
    rows.foreach {
      case Left(e: DynamoReadError) => error(e.toString)
      case _ => Unit
    }

    rows
      .filter { case Right(_) => true }
      .flatMap(_.right.toOption)
  }

  def updateRows(reindex: Reindex, rows: List[ReindexItem[String]]) = {
    val updateTable = reindex match {
      case Reindex("MiroData", _, _) => miroTable
      case Reindex("CalmData", _, _) => calmTable
      case _ =>
        throw new RuntimeException(
          s"Attempting to update unidentified table ${reindex.TableName}")
    }

    val ops = rows.map(reindexItem => {
      val uniqueKey = reindexItem.hashKey and reindexItem.rangeKey
      updateTable.update(uniqueKey,
                         set('ReindexVersion -> reindex.requestedVersion))
    })

    Future(ops.map(Scanamo.exec(dynamoDBClient)(_)))
  }

  def getRowsWithOldReindexVersion(reindex: Reindex) = Future {
    val query = reindex match {
      case Reindex("MiroData", _, _) =>
        Scanamo.queryIndex[MiroTransformable](dynamoDBClient) _
      case Reindex("CalmData", _, _) =>
        Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
      case _ => throw new RuntimeException("nope")
    }

    query(reindex.TableName, gsiName)(
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, reindex.requestedVersion)
        )
      ))
  }

  def getIndicesForReindex: Future[List[Reindex]] =
    getIndices.map(_.filter {
      case Reindex(_, requested, current) if requested > current => true
      case _ => false
    })

  def getIndices: Future[List[Reindex]] = Future {
    Scanamo.scan[Reindex](dynamoDBClient)(tableName).map {
      case Right(reindexes) => reindexes
      case _ => throw new RuntimeException("nope")
    }
  }
}
