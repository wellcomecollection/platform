package uk.ac.wellcome.platform.archive.archivist.bag

import java.io.InputStream

import cats.implicits._
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ArchiveJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.bag.BagDigestFileCreator
import uk.ac.wellcome.platform.archive.common.models.bagit.BagItemPath
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object ArchiveItemJobCreator {

  /** Returns a list of all the items inside a bag that the manifest(s)
    * refer to.
    *
    * If any of the manifests are incorrectly formatted, it returns an error.
    *
    */
  def createArchiveDigestItemJobs(job: ArchiveJob)
    : Either[ArchiveError[ArchiveJob], List[ArchiveDigestItemJob]] =
    job.bagManifestLocations
      .map { manifestLocation: BagItemPath =>
        ZipLocation(
          zipFile = job.zipFile,
          bagItemPath = manifestLocation
        )
      }
      .traverse { zipLocation =>
        parseArchiveDigestItemJobs(job, zipLocation)
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
                                         zipLocation: ZipLocation)
    : Either[ArchiveError[ArchiveJob], List[ArchiveDigestItemJob]] = {
    val value: Either[ArchiveError[ArchiveJob], InputStream] = ZipFileReader
      .maybeInputStream(zipLocation)
      .toRight(FileNotFoundError(zipLocation.bagItemPath.toString, job))

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
          BagDigestFileCreator
            .create(
              line.trim(),
              job,
              job.bagRootPathInZip,
              zipLocation.bagItemPath.toString)
            .map { bagItem =>
              ArchiveDigestItemJob(archiveJob = job, bagDigestItem = bagItem)
            }
        }
    }
  }

}
