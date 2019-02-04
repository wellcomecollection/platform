package uk.ac.wellcome.platform.archive.archivist.bag
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.bag.BagInfoParser
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemPath,
  BagLocation,
  BagPath,
  ExternalIdentifier
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

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
      .map { externalIdentifier =>
        ArchiveJob(
          externalIdentifier,
          zipFile,
          BagLocation(
            storageNamespace = config.uploadConfig.uploadNamespace,
            storagePrefix = config.uploadConfig.uploadPrefix,
            storageSpace = ingestBagRequest.storageSpace,
            bagPath = BagPath(externalIdentifier.toString)
          ),
          config = config.bagItConfig,
          bagManifestLocations = config.bagItConfig.digestNames.map {
            BagItemPath(_)
          }
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
    : Either[ArchiveError[IngestBagRequest], ExternalIdentifier] = {
    ZipFileReader
      .maybeInputStream(ZipLocation(zipFile, BagItemPath("bag-info.txt")))
      .toRight[ArchiveError[IngestBagRequest]](
        FileNotFoundError("bag-info.txt", ingestBagRequest))
      .flatMap { inputStream =>
        BagInfoParser
          .parseBagInfo(ingestBagRequest, inputStream)
          .map(_.externalIdentifier)
      }
  }
}
