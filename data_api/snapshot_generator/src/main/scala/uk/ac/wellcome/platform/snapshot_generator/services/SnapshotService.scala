package uk.ac.wellcome.platform.snapshot_generator.services

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.alpakka.s3.S3Exception
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.Inject
import org.elasticsearch.client.ResponseException
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.snapshot_generator.flow.{DisplayWorkToJsonStringFlow, IdentifiedWorkToVisibleDisplayWork, StringToGzipFlow}
import uk.ac.wellcome.platform.snapshot_generator.models.{CompletedSnapshotJob, SnapshotJob}
import uk.ac.wellcome.platform.snapshot_generator.source.ElasticsearchWorksSource
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.versions.ApiVersions

import scala.concurrent.Future

class SnapshotService @Inject()(actorSystem: ActorSystem,
                                akkaS3Client: S3Client,
                                elasticClient: HttpClient,
                                @Flag("aws.s3.endpoint") s3Endpoint: String,
                                @Flag("es.index.v1") esIndexV1: String,
                                @Flag("es.index.v2") esIndexV2: String,
                                @Flag("es.type") esType: String,
                                objectMapper: ObjectMapper)
    extends Logging {
//  val decider: Supervision.Decider = {
//    case _: S3Exception => Supervision.Stop
//    case _: ResponseException => Supervision.Stop
//    case _: Exception => Supervision.Resume
//  }
  implicit val system: ActorSystem = actorSystem
  implicit val materializer = ActorMaterializer()

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
          indexName = esIndexV1,
          toDisplayWork = DisplayWorkV1.apply
        )
      case ApiVersions.v2 =>
        runStream(
          publicBucketName = publicBucketName,
          publicObjectKey = publicObjectKey,
          indexName = esIndexV2,
          toDisplayWork = DisplayWorkV2.apply
        )
    }

    uploadResult.map { _ =>
      val targetLocation =
        Uri(s"$s3Endpoint/$publicBucketName/$publicObjectKey")

      CompletedSnapshotJob(
        snapshotJob = snapshotJob,
        targetLocation = targetLocation
      )
    }
  }

  private def runStream(
    publicBucketName: String,
    publicObjectKey: String,
    indexName: String,
    toDisplayWork: (IdentifiedWork, WorksIncludes) => DisplayWork)
    : Future[MultipartUploadResult] = {

    // This source outputs DisplayWorks in the elasticsearch index.
    val displayWorks: Source[DisplayWork, Any] =
      ElasticsearchWorksSource(elasticClient, indexName, esType)
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
