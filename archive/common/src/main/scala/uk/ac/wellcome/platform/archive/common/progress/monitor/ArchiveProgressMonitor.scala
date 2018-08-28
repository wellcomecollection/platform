package uk.ac.wellcome.platform.archive.common.progress.monitor

import java.time.Instant
import java.time.format.DateTimeFormatter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.{ArchiveIngestProgress, ProgressEvent}
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, DynamoNonFatalError}

class ArchiveProgressMonitor@Inject()(dynamoDbClient: AmazonDynamoDB,
                                      dynamoConfig: DynamoConfig)
  extends Logging {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException]( str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str))
    )(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def create(progress: ArchiveIngestProgress)= {
    val progressTable = Table[ArchiveIngestProgress](dynamoConfig.table)

    val ops = progressTable.given((not(attributeExists('id)))).put(progress)

    Scanamo.exec(dynamoDbClient)(ops)  match {
      case Left(e: ConditionalCheckFailedException) =>
        throw DynamoNonFatalError(e)
      case Left(scanamoError) =>
        val exception = new RuntimeException(scanamoError.toString)
        warn(s"Failed to update Dynamo record: ${progress.id}", exception)
        throw exception
      case Right(_) =>
        debug(s"Successfully updated Dynamo record: ${progress.id}")
    }
  }

  def addEvent(id: String, description: String) = {
    val progressTable = Table[ArchiveIngestProgress](dynamoConfig.table)

    val event = ProgressEvent(description, Instant.now())
    val ops = progressTable.update('id -> id, append('events -> event))

    Scanamo.exec(dynamoDbClient)(ops) match {
      case Left(scanamoError) =>
        val exception = new RuntimeException(scanamoError.toString)
        warn(s"Failed to update Dynamo record: $id", exception)
        throw exception
      case Right(_) =>
        debug(s"Successfully updated Dynamo record: $id")
    }
  }
}
