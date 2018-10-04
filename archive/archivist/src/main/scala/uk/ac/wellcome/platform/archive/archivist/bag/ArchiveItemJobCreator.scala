package uk.ac.wellcome.platform.archive.archivist.bag

import java.io.InputStream

import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  FileNotFoundError
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

object ArchiveItemJobCreator extends Logging {

  def createArchiveItemJobs(job: ArchiveJob, delimiter: String)
    : Either[ArchiveError[ArchiveJob], List[ArchiveItemJob]] = {
    val zipLocations = job.bagManifestLocations.map(manifestLocation =>
      ZipLocation(job.zipFile, manifestLocation.toEntryPath))
    zipLocations
      .traverse { zipLocation =>
        parseArchiveItemJobs(job, zipLocation, delimiter)
      }
      .map(_.flatten)
  }

  private def parseArchiveItemJobs(job: ArchiveJob,
                                   zipLocation: ZipLocation,
                                   delimiter: String)
    : Either[ArchiveError[ArchiveJob], List[ArchiveItemJob]] = {
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
        .filter(_.nonEmpty)
        .traverse { line =>
          BagItemCreator
            .create(line.trim(), job, zipLocation.entryPath.path, delimiter)
            .map(bagItem => ArchiveItemJob(job, bagItem))
        }
    }
  }

}
