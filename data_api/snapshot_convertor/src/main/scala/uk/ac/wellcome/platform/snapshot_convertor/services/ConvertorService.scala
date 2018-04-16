package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.Inject
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.platform.snapshot_convertor.flow.{
  DisplayWorkToJsonStringFlow,
  ElasticsearchHitToIdentifiedWorkFlow,
  IdentifiedWorkToVisibleDisplayWork,
  StringToGzipFlow
}
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.platform.snapshot_convertor.source.S3Source
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 s3Client: AmazonS3,
                                 akkaS3Client: S3Client,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String,
                                 objectMapper: ObjectMapper)
    extends Logging {

  implicit val materializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

    val publicBucketName = conversionJob.publicBucketName
    val targetObjectKey = conversionJob.targetObjectKey

    val uploadResult = for {
      s3inputStream <- Future {
        s3Client
          .getObject(
            conversionJob.privateBucketName,
            conversionJob.privateObjectKey)
          .getObjectContent
      }

      gzipStream <- runStream(
        publicBucketName = publicBucketName,
        targetObjectKey = targetObjectKey,
        s3inputStream = s3inputStream
      )
    } yield gzipStream

    uploadResult.map { _ =>
      val targetLocation =
        Uri(s"$s3Endpoint/$publicBucketName/$targetObjectKey")

      CompletedConversionJob(
        conversionJob = conversionJob,
        targetLocation = targetLocation
      )
    }
  }

  private def runStream(
    publicBucketName: String,
    targetObjectKey: String,
    s3inputStream: S3ObjectInputStream): Future[MultipartUploadResult] = {
    val s3source = S3Source(s3inputStream = s3inputStream)

    // This source generates instances of DisplayWork from the source snapshot.
    val displayWorks: Source[DisplayWorkV1, Any] = s3source
      .via(ElasticsearchHitToIdentifiedWorkFlow())
      .via(IdentifiedWorkToVisibleDisplayWork())

    // This source generates JSON strings of DisplayWork instances, which
    // should be written to the destination snapshot.
    val jsonStrings: Source[String, Any] = displayWorks
      .via(DisplayWorkToJsonStringFlow(mapper = objectMapper))

    // This source generates gzip-compressed JSON strings, corresponding to
    // the DisplayWork instances from the source snapshot.
    val gzipContent: Source[ByteString, Any] = jsonStrings
      .via(StringToGzipFlow())

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      akkaS3Client.multipartUpload(
        bucket = publicBucketName,
        key = targetObjectKey
      )

    gzipContent.runWith(s3Sink)
  }
}
