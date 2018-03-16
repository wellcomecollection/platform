package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import com.amazonaws.services.s3.AmazonS3
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.snapshot_convertor.models.ConversionJob
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ConvertorService @Inject()(s3Client: AmazonS3) extends Logging {

  def runConversion(conversionJob: ConversionJob): Future[Unit] = {
    info(s"ConvertorService running $conversionJob")

    Future.failed(new RuntimeException("Not implemented!"))
  }
}
