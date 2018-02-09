package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{VersionUpdater, VersionedDynamoFormatWrapper}
import uk.ac.wellcome.platform.reindexer.models.{
  ReindexJob,
  ScanamoQueryStream
}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               metricsSender: MetricsSender,
                               versionedDao: VersionedDao,
                               dynamoConfig: DynamoConfig)
    extends Logging {

  implicit val versionUpdater = new VersionUpdater[HybridRecord] {
    override def updateVersion(record: HybridRecord,
                               newVersion: Int): HybridRecord =
      record.copy(version = newVersion)
  }

  type ScanamoQueryResult = Either[DynamoReadError, HybridRecord]
  type ScanamoQueryResultFunction = (List[ScanamoQueryResult]) => Boolean

  private val gsiName = "reindexTracker"

  private def scanamoQueryStreamFunction(
    queryRequest: ScanamoQueryRequest,
    resultFunction: ScanamoQueryResultFunction
  )(implicit evidence: DynamoFormat[HybridRecord]): ScanamoOps[List[Boolean]] =
    ScanamoQueryStream.run[HybridRecord, Boolean](queryRequest, resultFunction)

  private def updateVersion(desiredVersion: Int)(
    resultGroup: List[ScanamoQueryResult])(
    implicit evidence: VersionedDynamoFormatWrapper[HybridRecord],
    versionUpdater: VersionUpdater[HybridRecord]): Boolean = {
    val updatedResults = resultGroup.map {
      case Left(e) => Left(e)
      case Right(hybridRecord) => {
        val existingRecord =
          versionedDao.getRecord[HybridRecord](id = hybridRecord.id)
        existingRecord.map { possibleRecord =>
          // getRecord() returns an Option[HybridRecord] because you may be
          // looking up a non-existent ID; since the ID came from the table we
          // assume the record exists.  It would be very unusual not to!
          val record = possibleRecord match {
            case Some(r) => r
            case None =>
              throw new RuntimeException(
                s"Asked to reindex a missing record ${hybridRecord.id}, but it's not in the table!"
              )
          }

          // TODO: what if desiredVersion < reindexVersion?
          val updatedRecord = record.copy(reindexVersion = desiredVersion)
          versionedDao.updateRecord[HybridRecord](updatedRecord)(
            evidence = evidence,
            versionUpdater = versionUpdater
          )
        }
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

  private def createScanamoQueryRequest(
    reindexJob: ReindexJob): ScanamoQueryRequest =
    ScanamoQueryRequest(
      dynamoConfig.table,
      Some(gsiName),
      Query(
        AndQueryCondition(
          KeyEquals('reindexShard, reindexJob.shardId),
          KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
        )),
      ScanamoQueryOptions.default
    )

  def runReindex(reindexJob: ReindexJob)(
    implicit evidence: DynamoFormat[HybridRecord]): Future[Unit] = Future {

    info(s"ReindexTargetService running $reindexJob")

    val scanamoQueryRequest = createScanamoQueryRequest(reindexJob)

    val ops = scanamoQueryStreamFunction(
      queryRequest = scanamoQueryRequest,
      resultFunction = updateVersion(reindexJob.desiredVersion)
    )

    val result: Seq[Boolean] = Scanamo.exec(dynamoDBClient)(ops)

    if (result.contains(false)) {
      throw GracefulFailureException(
        new RuntimeException(
          "Not all records were successfully processed!"
        ))
    } else {
      info(s"Successfully processed reindex job $reindexJob")
    }
  }
}
