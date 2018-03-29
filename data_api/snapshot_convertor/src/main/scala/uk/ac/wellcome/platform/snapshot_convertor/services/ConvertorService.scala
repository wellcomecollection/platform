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
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import akka.stream.scaladsl.{Compression, Sink}
import com.twitter.finatra.json.FinatraObjectMapper
import io.circe.parser.parse
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.snapshot_convertor.source.S3Source

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 awsConfig: AWSConfig,
                                 s3Client: S3Client,
                                 mapper: FinatraObjectMapper,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String)
    extends Logging {

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {

    info(s"ConvertorService running $conversionJob")

    val targetObjectKey = "target.txt.gz"

    val includes = WorksIncludes(
      identifiers = true,
      thumbnail = true,
      items = true
    )

    val s3source = S3Source(
      s3client = s3Client,
      bucketName = conversionJob.bucketName,
      key = conversionJob.objectKey
    )

    val source = s3source
      .map { sourceString =>
        parse(sourceString)
      }
      .collect {
        case Right(json) => Some(json)
        case Left(parseFailure) => {
          warn("Failed to parse work metatdata!", parseFailure)
          throw parseFailure
        }
      }
      .collect { case Some(json) => json }
      .map { json =>
        json \\ "_source" head
      }
      .map(_.as[IdentifiedWork])
      .collect {
        case Right(identifiedWork) => Some(identifiedWork)
        case Left(parseFailure) => {
          warn("Failed to parse identifiedWork!", parseFailure)
          throw parseFailure
        }
      }
      .collect { case Some(identifiedWork) => identifiedWork }
      .map { work =>
        DisplayWork(work, includes)
      }
      .map { mapper.writeValueAsString(_) }
      .map { ByteString(_) }
      .via(Compression.gzip)

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      s3Client.multipartUpload(
        bucket = conversionJob.bucketName,
        key = targetObjectKey
      )

    val future = source.runWith(s3Sink)(ActorMaterializer()(actorSystem))

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
