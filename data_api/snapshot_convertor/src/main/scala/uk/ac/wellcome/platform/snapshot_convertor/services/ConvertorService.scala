package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject
import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag

import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.platform.snapshot_convertor.flow.{
  ElasticsearchHitToDisplayWorkFlow,
  StringToGzipFlow
}
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.platform.snapshot_convertor.source.S3Source
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 s3Client: S3Client,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String)
    extends Logging {

  implicit val materializer = ActorMaterializer()(actorSystem)

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

    val s3source = S3Source(
      s3client = s3Client,
      bucketName = conversionJob.bucketName,
      key = conversionJob.objectKey
    )

    // This source generates instances of DisplayWork from the source snapshot.
    val displayWorks: Source[DisplayWork, Any] = s3source
      .via(ElasticsearchHitToDisplayWorkFlow())

    // This source generates JSON strings of DisplayWork instances, which
    // should be written to the destination snapshot.
    val jsonStrings: Source[String, Any] = displayWorks
      .map { displayWork: DisplayWork =>
        toJson(displayWork)
      }
      .map {
        case Success(jsonString) => jsonString
        case Failure(encodeError) => {
          warn("Failed to convert $displayWork to string!", encodeError)
          throw encodeError
        }
      }

    // This source generates gzip-compressed JSON strings, corresponding to
    // the DisplayWork instances from the source snapshot.
    val gzipContent: Source[ByteString, Any] = jsonStrings
      .via(StringToGzipFlow())

    val targetObjectKey = "target.txt.gz"

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      s3Client.multipartUpload(
        bucket = conversionJob.bucketName,
        key = targetObjectKey
      )

    val future = gzipContent
      .runWith(s3Sink)

    future.map { _ =>
      val targetLocation =
        Uri(s"$s3Endpoint/${conversionJob.bucketName}/$targetObjectKey")

      CompletedConversionJob(
        conversionJob = conversionJob,
        targetLocation = targetLocation
      )
    }
  }
}
