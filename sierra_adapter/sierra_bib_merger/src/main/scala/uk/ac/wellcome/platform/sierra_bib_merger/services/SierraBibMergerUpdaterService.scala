package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.google.inject.Inject
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.syntax._
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.SierraBibRecord._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(dynamoDBClient: AmazonDynamoDB,
                                              metrics: MetricsSender,
                                              dynamoConfig: DynamoConfig)
    extends Logging {

  def update(mergedSierraRecord: MergedSierraRecord): Unit = {
    val table = Table[MergedSierraRecord](dynamoConfig.table)
    val ops = table
      .given(
        not(attributeExists('id))
      )
      .put(mergedSierraRecord)
    val x = Scanamo.exec(dynamoDBClient)(ops) match {
      case Right(_) =>
        logger.info(s"$mergedSierraRecord saved successfully to DynamoDB")
      case Left(error) =>
        logger.warn(s"Failed saving $mergedSierraRecord to DynamoDB", error)
    }
  }
}
