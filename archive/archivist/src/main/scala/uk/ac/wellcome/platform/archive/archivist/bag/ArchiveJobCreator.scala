package uk.ac.wellcome.platform.archive.archivist.bag
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.archivist.models.errors.{ArchiveError, FileNotFoundError, InvalidBagInfo}
import uk.ac.wellcome.platform.archive.archivist.modules.BagUploaderConfig
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.models._

object ArchiveJobCreator {
  def create(
    zipFile: ZipFile,
    config: BagUploaderConfig,
    ingestBagRequest: IngestBagRequest
  ): Either[ArchiveError[IngestBagRequest], ArchiveJob] = {

    getBagIdentifier(zipFile, ingestBagRequest)
      .map(bagIdentifier => BagPath(s"$DigitisedStorageType/$bagIdentifier"))
      .map { bagPath =>
        ArchiveJob(
          zipFile,
          BagLocation(
            storageNamespace = config.uploadConfig.uploadNamespace,
            storagePath = config.uploadConfig.uploadPrefix,
            bagPath = bagPath
          ),
          config.bagItConfig,
          BagManifestLocation.create(config.bagItConfig)
        )
      }
  }

  private def getBagIdentifier(zipFile: ZipFile,
                               ingestBagRequest: IngestBagRequest)
    : Either[ArchiveError[IngestBagRequest], String] = {
    ZipFileReader
      .maybeInputStream(ZipLocation(zipFile, EntryPath("bag-info.txt")))
      .toRight[ArchiveError[IngestBagRequest]](
        FileNotFoundError("bag-info.txt", ingestBagRequest))
      .flatMap { inputStream =>
        val bagInfoLines = scala.io.Source
          .fromInputStream(inputStream, "UTF-8")
          .mkString
          .split("\n")
        val regex = """(.*?)\s*:\s*(.*)\s*""".r

        bagInfoLines
          .collectFirst {
            case regex(key, value) if key == "External-Identifier" => value
          }
          .toRight(InvalidBagInfo(ingestBagRequest))
      }
  }
}
