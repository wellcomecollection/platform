package uk.ac.wellcome.platform.archive.archivist.bag

import java.io.InputStream

import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ArchiveJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object ArchiveItemJobCreator extends Logging {

  /** Returns a list of all the items inside a bag that the manifest(s)
    * refer to.
    *
    * If any of the manifests are incorrectly formatted, it returns an error.
    *
    */
  def createArchiveDigestItemJobs(job: ArchiveJob, delimiter: String)
    : Either[ArchiveError[ArchiveJob], List[ArchiveDigestItemJob]] =
    job.bagManifestLocations
      .map { manifestLocation =>
        ZipLocation(job.zipFile, manifestLocation.toEntryPath)
      }
      .traverse { zipLocation =>
        parseArchiveDigestItemJobs(job, zipLocation, delimiter)
      }
      .map { _.flatten }

  /** Given the location of a single manifest inside the BagIt bag,
    * return a list of all the items inside the bag that the manifest
    * refers to.
    *
    * If the manifest is incorrectly formatted, it returns an error.
    *
    */
  private def parseArchiveDigestItemJobs(job: ArchiveJob,
                                         zipLocation: ZipLocation,
                                         delimiter: String)
    : Either[ArchiveError[ArchiveJob], List[ArchiveDigestItemJob]] = {
    val value: Either[ArchiveError[ArchiveJob], InputStream] = ZipFileReader
      .maybeInputStream(zipLocation)
      .toRight(FileNotFoundError(zipLocation.entryPath.path, job))

    value.flatMap { inputStream =>
      val manifestFileLines: List[String] =
        scala.io.Source
          .fromInputStream(inputStream)
          .mkString
          .split("\n")
          .toList
      manifestFileLines
        .filter { _.nonEmpty }
        .traverse { line =>
          BagItemCreator
            .create(line.trim(), job, zipLocation.entryPath.path, delimiter)
            .map { bagItem =>
              ArchiveDigestItemJob(archiveJob = job, bagDigestItem = bagItem)
            }
        }
    }
  }

}
