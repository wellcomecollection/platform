package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SierraBibRecord._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{MergedSierraRecord, SierraBibRecord}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class SierraBibMergerUpdaterService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metrics: MetricsSender,
  dynamoConfig: DynamoConfig
) extends Logging {

  private val table = Table[MergedSierraRecord](dynamoConfig.table)

  private def putRecord(record: MergedSierraRecord) = table
    .given(
      not(attributeExists('id)) or
        (attributeExists('id) and 'version < record.version)
    )
    .put(record)

  def update(bibRecord: SierraBibRecord): Future[Unit] = Future {
    val existingRecord = Scanamo.exec(dynamoDBClient)(
      table.get('id -> bibRecord.id)
    )

    val newRecord = existingRecord.map {
      case Left(error) => Left(
        new RuntimeException(error.toString)
      )
      case Right(record) => record.mergeBibRecord(bibRecord).toRight(
        new RuntimeException("Unable to merge record!")
      )
    }.getOrElse(
      Right(MergedSierraRecord(id = bibRecord.id))
    )

    val putOperation = newRecord match {
      case Right(record) => Scanamo.exec(dynamoDBClient)(
        putRecord(record)
      ).left.map(e => new RuntimeException(e.toString))
      case Left(e) => Left(e)
    }

    putOperation match {
      case Right(_) =>
        logger.info(s"${bibRecord.id} saved successfully to DynamoDB")
      case Left(error) =>
        logger.warn(s"Failed processing ${bibRecord.id}", error)
    }
  }
}
