package uk.ac.wellcome.platform.archive.archivist.bag

import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

import scala.util.Try

object ArchiveItemJobCreator extends Logging {

  def createArchiveItemJobs(job: ArchiveJob, delimiter: String): Either[ArchiveJob, List[ArchiveItemJob]] = {
    val zipFile = job.zipFile
    val zipLocations = job.bagManifestLocations.map(manifestLocation => ZipLocation(zipFile, manifestLocation.toObjectLocation))
    val triedArchiveItemJobLists: Try[List[List[ArchiveItemJob]]] = zipLocations.map { zipLocation =>
      parseArchiveItemJobs(job, zipLocation, delimiter)
    }.sequence

    triedArchiveItemJobLists.map(_.flatten).toEither.leftMap(_ => job)
  }

  private def parseArchiveItemJobs(
                                    job: ArchiveJob,
                                    zipLocation: ZipLocation, delimiter: String): Try[List[ArchiveItemJob]] = {
    Try(ZipFileReader.maybeInputStream(zipLocation).get).flatMap {
      inputStream =>
        val manifestFileLines =
          scala.io.Source.fromInputStream(inputStream).mkString.split("\n")
        val triedArchiveItemJobs: List[Try[ArchiveItemJob]] = manifestFileLines
          .filter(_.nonEmpty)
          .map { line =>
            BagItemCreator
              .create(line.trim(), job.bagLocation.bagPath, delimiter).map(bagItem => ArchiveItemJob(
              job,bagItem))
          }
          .toList
        triedArchiveItemJobs.sequence
    }
  }

}
