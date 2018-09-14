package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{BagUploaderConfig, IngestRequestContext}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagName}

import scala.util.{Failure, Success, Try}


object UploadAndVerifyBagFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    s3Client: S3Client,
    actorSystem: ActorSystem
  ): Flow[(ZipFile, IngestRequestContext),
    (BagLocation, IngestRequestContext),
    NotUsed] = {

    val decider: Supervision.Decider = {
      case e => {
        error("UploadAndVerifyBagFlow stream failure", e)
        Supervision.Stop
      }
    }

    val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )

    Flow[(ZipFile, IngestRequestContext)].flatMapConcat {
      case (zipFile, ingestRequestContext) => {

        val f = Source
          .single(zipFile)
          .map(createArchiveJob(_, config))
          .flatMapConcat(ArchiveBagFlow(_))
          .groupBy(Int.MaxValue, _.bagLocation.bagName)
          .fold(List.empty[ArchiveJob])((list, o) => o :: list)
          .mergeSubstreams
          .map(bags => (
            bags.head.bagLocation,
            ingestRequestContext
          ))
          .runWith(Sink.head[(BagLocation, IngestRequestContext)])(
            materializer
          )

        Source.fromFuture(f)

      }
    }
  }

  private def createArchiveJob(
                                zipFile: ZipFile,
                                config: BagUploaderConfig
                              ) = {
    val bagName = getBagName(zipFile)

    val bagLocation = BagLocation(
      storageNamespace = config.uploadConfig.uploadNamespace,
      storagePath = config.uploadConfig.uploadPrefix,
      bagName = bagName
    )

    ArchiveJob(zipFile, bagLocation, config.bagItConfig)
  }

  private def getBagName(zipFile: ZipFile) = {
    val entries = zipFile.entries()

    Stream
      .continually(entries.nextElement)
      .map(_.getName.split("/"))
      .filter(_.length > 1)
      .flatMap(_.headOption)
      .takeWhile(_ => entries.hasMoreElements)
      .toSet
      .filterNot(_.startsWith("_"))
      .map(BagName)
      .head
  }
}
