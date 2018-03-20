package uk.ac.wellcome.platform.snapshot_convertor.services

import javax.inject.Inject

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.util.ByteString
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.AwsRegionProvider
import com.twitter.inject.annotations.Flag
import io.circe.Json
import io.circe.parser.parse
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import akka.stream.scaladsl.{Compression, Framing, Keep, Sink, Source}


class ConvertorService @Inject()(@Flag("aws.s3.bucketName") bucketName: String,
                                 actorSystem: ActorSystem,
                                 awsConfig: AWSConfig)
    extends Logging {

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

    val credentialsProvider = DefaultAWSCredentialsProviderChain
      .getInstance()

    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsConfig.region
      }

    val settings = new S3Settings(
      bufferType = MemoryBufferType,
      proxy = None,
      credentialsProvider = credentialsProvider,
      s3RegionProvider = regionProvider,
      pathStyleAccess = false,
      endpointUrl = None
    )

    val actorMaterializer = ActorMaterializer()(actorSystem)
    val objectKey = conversionJob.objectKey
    val targetObjectKey = "target.txt.gz"

    val s3Client = new S3Client(settings)(actorSystem, actorMaterializer)
    val (s3Source: Source[ByteString, _], _) = s3Client.download(bucketName, objectKey)

    val includes = WorksIncludes(
      identifiers = true,
      thumbnail = true,
      items = true
    )

    val uncompressedSource = s3Source
      .via(Compression.gunzip())
      .via(Framing.delimiter(ByteString("."), Int.MaxValue))
      .map(_.utf8String)
      .map(JsonUtil.fromJson[IdentifiedWork](_).get)
      .map(work => DisplayWork(work, includes))
      .map(JsonUtil.toJson(_).get)
      .map(ByteString(_))
      .via(Compression.gzip)

    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
      s3Client.multipartUpload(bucketName, targetObjectKey)

    val runnable = s3Source.toMat(s3Sink)(Keep.right)

    val future = runnable.run()(actorMaterializer)

    // this should be an app variable
    val host = "http://localhost:33333"
    val targetLocation = Uri(s"$host/$objectKey")

    future.map { _ =>
      CompletedConversionJob(
        conversionJob,
        targetLocation
      )
    }
  }
}
