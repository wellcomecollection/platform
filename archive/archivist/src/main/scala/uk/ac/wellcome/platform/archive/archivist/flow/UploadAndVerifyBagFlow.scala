package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{BagUploaderConfig, IngestRequestContext}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagName}

object SplitAndGroupArchiveJobFlow {
  def apply()(implicit s3Client: S3Client) = Flow[ArchiveJob]
    .flatMapConcat(ArchiveBagSource(_))
    .groupBy(Int.MaxValue, {
      case Right(archiveItemJob) => archiveItemJob.bagName
      case Left(archiveItemJob) => archiveItemJob.bagName
    })
    .reduce((first, second) => if(first.isLeft) first else second)
    .mergeSubstreams
    .collect {
      case Right(archiveItemJob) => archiveItemJob.archiveJob
    }
}

object UploadAndVerifyBagFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    s3Client: S3Client,
    actorSystem: ActorSystem
  ): Flow[(ZipFile, IngestRequestContext), (BagLocation, IngestRequestContext), NotUsed] = {


    Flow[(ZipFile, IngestRequestContext)].flatMapConcat {
      case (zipFile, ingestRequestContext) => {
        Source
          .single(zipFile)
          .map(createArchiveJob(_, config))
          .via(SplitAndGroupArchiveJobFlow())
          .map(job =>
            (job.bagLocation, ingestRequestContext)
          )

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
