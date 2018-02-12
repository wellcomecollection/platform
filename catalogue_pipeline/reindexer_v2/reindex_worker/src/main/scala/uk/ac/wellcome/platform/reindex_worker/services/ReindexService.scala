package uk.ac.wellcome.platform.reindex_worker.services

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
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ScanamoQueryStream}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.immutable
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
  type ScanamoQueryResultFunction = (List[ScanamoQueryResult]) => Future[Boolean]

  private val gsiName = "reindexTracker"

  private def scanamoQueryStreamFunction(
    queryRequest: ScanamoQueryRequest,
    resultFunction: ScanamoQueryResultFunction
  )(implicit evidence: DynamoFormat[HybridRecord]): ScanamoOps[List[Future[Boolean]]] =
    ScanamoQueryStream.run[HybridRecord, Future[Boolean]](queryRequest, resultFunction)

  private def getRecord(hybridRecord: HybridRecord): Future[HybridRecord] = {
    val existingRecord =
      versionedDao.getRecord[HybridRecord](id = hybridRecord.id)

    existingRecord.map {
      optionalRecord: Option[HybridRecord] => optionalRecord match {
        case Some(r) => r
        case None => throw new RuntimeException(
          s"Asked to reindex a missing record ${hybridRecord.id}, but it's not in the table!"
        )
      }
    }
  }

  private def updateRecord(
    hybridRecord: HybridRecord,
    desiredVersion: Int
  )(
    implicit evidence: VersionedDynamoFormatWrapper[HybridRecord],
    versionUpdater: VersionUpdater[HybridRecord]
  ): Future[Unit] = {
    // TODO: what if desiredVersion < reindexVersion?
    val updatedRecord = hybridRecord.copy(reindexVersion = desiredVersion)

    versionedDao.updateRecord[HybridRecord](updatedRecord)(
      evidence = evidence,
      versionUpdater = versionUpdater
    )
  }

  def updateQueryResults(desiredVersion: Int)(
    resultGroup: List[ScanamoQueryResult]
  )(
    implicit evidence: VersionedDynamoFormatWrapper[HybridRecord],
    versionUpdater: VersionUpdater[HybridRecord]
  ): Future[Boolean] = {

    val updates: Seq[Future[Unit]] = resultGroup.map {
      case Left(e: DynamoReadError) => Future.failed(
        new Exception(
          s"DynamoReadError: ${DynamoReadError.describe(e)}"
        )
      )
      case Right(hybridRecord: HybridRecord) => {
        getRecord(hybridRecord).flatMap { record =>
          updateRecord(record, desiredVersion)
        }
      }
    }

    Future.sequence(updates).map(_ => {
      info(s"ReindexTargetService completed batch of ${updates.length}")

      metricsSender.incrementCount(
        "reindex-updated-items",
        updates.length
      )

      true
    }).recover {
      case _ => false
    }
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
    implicit evidence: DynamoFormat[HybridRecord]): Future[Unit] = {

    info(s"ReindexTargetService running $reindexJob")

    val scanamoQueryRequest = createScanamoQueryRequest(reindexJob)

    val ops: ScanamoOps[List[Future[Boolean]]] = scanamoQueryStreamFunction(
      queryRequest = scanamoQueryRequest,
      resultFunction = updateQueryResults(reindexJob.desiredVersion)
    )

    Future.sequence(Scanamo.exec(dynamoDBClient)(ops)).map( result =>
      if (result.contains(false)) {
        throw GracefulFailureException(
          new RuntimeException(
            "Not all records were successfully processed!"
          ))
      } else {
        info(s"Successfully processed reindex job $reindexJob")
      }
    )
  }
}
