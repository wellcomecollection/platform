package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.Inject
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.snapshot_convertor.flow.{DisplayWorkToJsonStringFlow, IdentifiedWorkToVisibleDisplayWork, StringToGzipFlow}
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.platform.snapshot_convertor.source.ElasticsearchSource
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.versions.ApiVersions

import scala.concurrent.Future

class ConvertorService @Inject()(actorSystem: ActorSystem,
                                 akkaS3Client: S3Client,
                                 elasticClient: HttpClient,
                                 @Flag("aws.s3.endpoint") s3Endpoint: String,
                                 @Flag("es.index.v1") esIndexV1: String,
                                 @Flag("es.index.v2") esIndexV2: String,
                                 @Flag("es.type") esType: String,
                                 objectMapper: ObjectMapper)
    extends Logging {

  implicit val system: ActorSystem = actorSystem
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)

  def runConversion(
    conversionJob: ConversionJob): Future[CompletedConversionJob] = {
    info(s"ConvertorService running $conversionJob")

    val publicBucketName = conversionJob.publicBucketName
    val publicObjectKey = conversionJob.publicObjectKey

    val uploadResult = runStream(
        publicBucketName = publicBucketName,
        publicObjectKey = publicObjectKey,
        apiVersion = conversionJob.apiVersion
    )

    uploadResult.map { _ =>
      val targetLocation =
        Uri(s"$s3Endpoint/$publicBucketName/$publicObjectKey")

      CompletedConversionJob(
        conversionJob = conversionJob,
        targetLocation = targetLocation
      )
    }
  }

  private def runStream(
    publicBucketName: String,
    publicObjectKey: String,
    apiVersion: ApiVersions.Value): Future[MultipartUploadResult] = {

    val toDisplayWork: ((IdentifiedWork, WorksIncludes) => DisplayWork) =
      apiVersion match {
        case ApiVersions.v1 => DisplayWorkV1.apply
        case ApiVersions.v2 => DisplayWorkV2.apply
      }
    val indexName = apiVersion match {
      case ApiVersions.v1 => esIndexV1
      case ApiVersions.v2 => esIndexV2
    }

    // This source generates instances of DisplayWork from the source snapshot.
    val displayWorks: Source[DisplayWork, Any] = ElasticsearchSource(elasticClient, indexName, esType)
      .via(IdentifiedWorkToVisibleDisplayWork(toDisplayWork))

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
        key = publicObjectKey
      )

    gzipContent.runWith(s3Sink)
  }
}
