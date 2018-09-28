package uk.ac.wellcome.platform.archive.archivist.bag

import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

import scala.util.Try

object ArchiveItemJobCreator extends Logging {

  def createArchiveItemJobs(
    job: ArchiveJob,
    delimiter: String): Either[ArchiveJob, List[ArchiveItemJob]] = {
    val zipLocations = job.bagManifestLocations.map(manifestLocation =>
      ZipLocation(job.zipFile, manifestLocation.toEntryPath))
    zipLocations
      .traverse { zipLocation =>
        parseArchiveItemJobs(job, zipLocation, delimiter)
      }
      .map(_.flatten)
      .toEither
      .leftMap(ex => {
        error(s"Failed creating archive item jobs from $job", ex)
        job
      })
  }

  private def parseArchiveItemJobs(
    job: ArchiveJob,
    zipLocation: ZipLocation,
    delimiter: String): Try[List[ArchiveItemJob]] = {
    Try(ZipFileReader.maybeInputStream(zipLocation).get).flatMap {
      inputStream =>
        val manifestFileLines =
          scala.io.Source
            .fromInputStream(inputStream)
            .mkString
            .split("\n")
            .toList
        manifestFileLines
          .filter(_.nonEmpty)
          .traverse { line =>
            BagItemCreator
              .create(line.trim(), job.bagLocation.bagPath, delimiter)
              .map(bagItem => ArchiveItemJob(job, bagItem))
          }
    }
  }

}
