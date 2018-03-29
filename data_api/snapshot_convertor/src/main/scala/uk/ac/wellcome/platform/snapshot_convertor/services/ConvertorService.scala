package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.util.ByteString
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.aws.AWSConfig

import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.snapshot_convertor.flow.{
  ElasticsearchHitToDisplayWorkFlow,
  StringToGzipFlow
}
import uk.ac.wellcome.platform.snapshot_convertor.source.S3Source

import scala.util.{Success, Failure}

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 awsConfig: AWSConfig,
                                 s3Client: S3Client,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String)
    extends Logging {

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {

    info(s"ConvertorService running $conversionJob")

    val targetObjectKey = "target.txt.gz"

    val s3source = S3Source(
      s3client = s3Client,
      bucketName = conversionJob.bucketName,
      key = conversionJob.objectKey
    )

    val displayWorks = s3source
      .via(ElasticsearchHitToDisplayWorkFlow())

    val jsonStrings = displayWorks
      .map { displayWork: DisplayWork => toJson(displayWork) }
      .map {
        case Success(jsonString) => jsonString
        case Failure(encodeError) => {
          warn("Failed to convert $displayWork to string!", encodeError)
          throw encodeError
        }
      }

    val gzipContent = jsonStrings
      .via{ StringToGzipFlow(_) }

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      s3Client.multipartUpload(
        bucket = conversionJob.bucketName,
        key = targetObjectKey
      )

    val future = gzipContent.runWith(s3Sink)(ActorMaterializer()(actorSystem))

    future.map { result =>
      val targetLocation =
        Uri(s"$s3Endpoint/${conversionJob.bucketName}/$targetObjectKey")

      CompletedConversionJob(
        conversionJob,
        targetLocation
      )
    }
  }
}
