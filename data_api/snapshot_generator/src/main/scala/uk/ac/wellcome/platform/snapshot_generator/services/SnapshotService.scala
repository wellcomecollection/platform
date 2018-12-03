package uk.ac.wellcome.platform.snapshot_generator.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.Logging
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.platform.snapshot_generator.flow.{
  DisplayWorkToJsonStringFlow,
  IdentifiedWorkToVisibleDisplayWork,
  StringToGzipFlow
}
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.source.ElasticsearchWorksSource

import scala.concurrent.{ExecutionContext, Future}

class SnapshotService @Inject()(akkaS3Client: S3Client,
                                elasticClient: HttpClient,
                                elasticConfig: DisplayElasticConfig,
                                objectMapper: ObjectMapper)(
  implicit actorSystem: ActorSystem,
  ec: ExecutionContext
) extends Logging {
  implicit val materializer = ActorMaterializer()

  val s3Endpoint = akkaS3Client.s3Settings.endpointUrl.getOrElse("s3:/")

  def buildLocation(bucketName: String, objectKey: String): Uri =
    Uri(s"$s3Endpoint/$bucketName/$objectKey")

  def generateSnapshot(
    snapshotJob: SnapshotJob): Future[CompletedSnapshotJob] = {
    info(s"ConvertorService running $snapshotJob")

    val publicBucketName = snapshotJob.publicBucketName
    val publicObjectKey = snapshotJob.publicObjectKey

    val uploadResult = snapshotJob.apiVersion match {
      case ApiVersions.v1 =>
        runStream(
          publicBucketName = publicBucketName,
          publicObjectKey = publicObjectKey,
          indexName = elasticConfig.indexV1name,
          toDisplayWork = DisplayWorkV1.apply(_, V1WorksIncludes.includeAll())
        )
      case ApiVersions.v2 =>
        runStream(
          publicBucketName = publicBucketName,
          publicObjectKey = publicObjectKey,
          indexName = elasticConfig.indexV2name,
          toDisplayWork = DisplayWorkV2.apply(_, V2WorksIncludes.includeAll())
        )
    }

    uploadResult.map { _ =>
      val targetLocation = buildLocation(
        bucketName = publicBucketName,
        objectKey = publicObjectKey
      )

      CompletedSnapshotJob(
        snapshotJob = snapshotJob,
        targetLocation = targetLocation
      )
    }
  }

  private def runStream(publicBucketName: String,
                        publicObjectKey: String,
                        indexName: String,
                        toDisplayWork: IdentifiedWork => DisplayWork)
    : Future[MultipartUploadResult] = {

    // This source outputs DisplayWorks in the elasticsearch index.
    val displayWorks: Source[DisplayWork, Any] =
      ElasticsearchWorksSource(
        elasticClient = elasticClient,
        indexName = indexName)
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
