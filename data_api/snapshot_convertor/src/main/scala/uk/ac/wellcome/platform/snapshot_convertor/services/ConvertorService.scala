package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, _}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindex_worker.models.{
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ConvertorService @Inject()() extends Logging {

  def runConversion(conversionJob: ConversionJob): Future[Unit] = {
    info(s"ConvertorService running $conversionJob")

    Future.failed(new RuntimeException("Not implemented!"))
  }
}
