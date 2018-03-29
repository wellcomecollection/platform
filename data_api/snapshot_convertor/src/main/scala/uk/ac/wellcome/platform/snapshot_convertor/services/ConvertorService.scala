package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject
import scala.concurrent.Future
import scala.util.{Success, Failure}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag

import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.platform.snapshot_convertor.flow.{
  ElasticsearchHitToDisplayWorkFlow,
  StringToGzipFlow
}
import uk.ac.wellcome.platform.snapshot_convertor.models.ConversionJob
import uk.ac.wellcome.platform.snapshot_convertor.source.S3Source
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 s3Client: S3Client,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String) extends Logging {

  implicit val materializer = ActorMaterializer()(actorSystem)

  def runConversion(conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

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

    val targetObjectKey = "target.txt.gz"

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      s3Client.multipartUpload(
        bucket = conversionJob.bucketName,
        key = targetObjectKey
      )

    val future = gzipContent
      .runWith(s3Sink)

    future.map { _ =>
      val targetLocation = Uri(s"$s3endpoint/${conversionJob.bucketName}/$targetObjectKey")

      CompletedConversionJob(
        conversionJob = conversionJob,
        targetLocation = targetLocation
      )
    }
  }
}
