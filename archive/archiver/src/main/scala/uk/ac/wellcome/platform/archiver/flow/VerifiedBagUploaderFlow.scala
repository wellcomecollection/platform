package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

// TODO: Verify checksums in S3 are what you set them to
object VerifiedBagUploaderFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    materializer: ActorMaterializer,
    s3Client: S3Client
  ): Flow[ZipFile, Seq[Done], NotUsed] = {

    val digestLocationFlow: Flow[ZipFile, ObjectLocation, NotUsed] = DigestLocationFlow(config)

    Flow[ZipFile].flatMapConcat((zipFile) => {
      Source.single(zipFile)
        .via(digestLocationFlow)
        .flatMapConcat(digestLocation => {

          val bagName = BagName(digestLocation.namespace)
          val verifiedDigestUploaderFlow = VerifiedDigestUploaderFlow(zipFile, bagName, config)

          Source.fromFuture(
            Source.single(digestLocation)
              .via(verifiedDigestUploaderFlow)
              .runWith(Sink.seq)
          )

        })
    })
  }
}

