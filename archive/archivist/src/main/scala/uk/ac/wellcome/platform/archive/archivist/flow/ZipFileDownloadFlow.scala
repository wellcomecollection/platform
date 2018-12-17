package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.ZipFileDownloadComplete
import uk.ac.wellcome.platform.archive.archivist.models.errors.ZipFileDownloadingError
import uk.ac.wellcome.platform.archive.archivist.utils.TemporaryStore
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.progress.models._

/** This flow takes an ingest request, and downloads the entire ZIP file
  * associated with the request to a local (temporary) path.
  *
  * It returns an Either with errors on the Left, or the zipfile and the
  * original request on the Right.
  *
  */

object ZipFileDownloadFlow extends Logging {

  import TemporaryStore._

  import uk.ac.wellcome.platform.archive.common.ConvertibleToInputStream._

  def apply(parallelism: Int, snsConfig: SNSConfig)(implicit s3Client: AmazonS3,
                                                    snsClient: AmazonSNS)
  : Flow[IngestBagRequest,
    Either[ArchiveError[IngestBagRequest], ZipFileDownloadComplete],
    NotUsed] = {

    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](
      snsClient,
      snsConfig,
      subject = "archivist_progress"
    )

    Flow[IngestBagRequest]
      .flatMapMerge(
        parallelism, {
          case request @ IngestBagRequest(_, location: ObjectLocation, _, _) =>
            location.toInputStream match {
              case Failure(ex) =>
                warn(s"Failed downloading zipFile from $location with $ex")
                Source.single(Left(ZipFileDownloadingError(request, ex)))
              case Success(tmpFile) =>
                Source.single(
                  Right(
                    ZipFileDownloadComplete(
                      zipFile = new ZipFile(tmpFile),
                      ingestBagRequest = request
                    ))
                )
              )
            )

            Source
              .single(updates)
              .via(snsPublishFlow)
              .map(_ => results)
              .withAttributes(ActorAttributes.dispatcher(
                "akka.stream.materializer.blocking-io-dispatcher")
              )

        }
      )
  }
}