package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.util.ByteString
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import akka.stream.scaladsl.{Compression, Framing, Keep, Sink, Source}
import com.twitter.finatra.json.FinatraObjectMapper
import io.circe.parser.parse
import com.twitter.inject.annotations.Flag

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 awsConfig: AWSConfig,
                                 s3Client: S3Client,
                                 mapper: FinatraObjectMapper,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String)
    extends Logging {

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {

    info(s"ConvertorService running $conversionJob")

    val objectKey = conversionJob.objectKey
    val targetObjectKey = "target.txt.gz"

    val (s3Source: Source[ByteString, _], _) = s3Client.download(
      bucket = conversionJob.bucketName,
      key = objectKey
    )

    val includes = WorksIncludes(
      identifiers = true,
      thumbnail = true,
      items = true
    )

    val source = s3Source
      .via(Compression.gunzip())
      .via(Framing
        .delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true))
      .map { _.utf8String }
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
