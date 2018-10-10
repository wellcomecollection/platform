package uk.ac.wellcome.platform.archive.archivist.bag
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  FileNotFoundError,
  InvalidBagInfo
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.models._

/** This flow extracts the external identifier from the metadata inside
  * the ZIP file, and emits an ArchiveJob.
  *
  * If it's unable to find the identifier, it emits a Left[ArchiveError] instead.
  *
  */
object ArchiveJobCreator {
  def create(
    zipFile: ZipFile,
    config: BagUploaderConfig,
    ingestBagRequest: IngestBagRequest
  ): Either[ArchiveError[IngestBagRequest], ArchiveJob] = {

    getBagIdentifier(zipFile, ingestBagRequest)
      .map { bagIdentifier =>
        BagPath(s"$DigitisedStorageType/$bagIdentifier")
      }
      .map { bagPath =>
        ArchiveJob(
          zipFile = zipFile,
          bagLocation = BagLocation(
            storageNamespace = config.uploadConfig.uploadNamespace,
            storagePath = config.uploadConfig.uploadPrefix,
            bagPath = bagPath
          ),
          config = config.bagItConfig,
          bagManifestLocations = BagManifestLocation.create(config.bagItConfig)
        )
      }
  }

  /** The ZIP files contain a "bag-info.txt" metadata file, with
    * lines of the form:
    *
    *     Bagging-Date: 2018-08-24
    *     Contact-Name: Henry Wellcome
    *     External-Identifier: 1234
    *
    * This method extracts the "External-Identifier" field from this metadata,
    * if present.
    *
    * In the BagIt spec, all these fields are optional, so it may not exist --
    * but that's an error for our use case!  We use the External-Identifier to
    * determine where to store the bag in S3, so its absence means we're
    * unable to proceed.
    *
    */
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
