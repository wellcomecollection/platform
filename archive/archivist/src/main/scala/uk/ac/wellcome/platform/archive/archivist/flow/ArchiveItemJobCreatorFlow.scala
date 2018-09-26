package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.BagItemCreator
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

object ArchiveItemJobCreatorFlow extends Logging {

  def apply(delimiter: String)
    : Flow[ArchiveJob, Either[ArchiveJob, ArchiveItemJob], NotUsed] =
    Flow[ArchiveJob]
      .log("digest location")
      .flatMapConcat {
        case job@ArchiveJob(zipFile, _, _, manifestLocations) =>
          Source(manifestLocations.map(manifestLocation => ZipLocation(zipFile, manifestLocation.toObjectLocation)))
            .map(ZipFileReader.maybeInputStream).map(_.toRight(()))
            .via(FoldEitherFlow[Unit, InputStream, Either[ArchiveJob, ArchiveItemJob]](_ => Left(job))(itemJobCreatorFlow(job, delimiter)))

      }
      .log("bag digest item")

  private def itemJobCreatorFlow(job: ArchiveJob, delimiter: String)
    : Flow[InputStream, Either[ArchiveJob, ArchiveItemJob], NotUsed]  ={
    Flow[InputStream]
      .mapConcat[Either[ArchiveJob, ArchiveItemJob]]{inputStream =>
        val manifestFileLines = scala.io.Source.fromInputStream(inputStream).mkString.split("\n")
        manifestFileLines.filter(_.nonEmpty).map{
          line =>
            BagItemCreator
              .create(line.trim(), job.bagLocation.bagPath, delimiter)
              .toEither.map( bagItem => ArchiveItemJob(job, bagItem)).leftMap(_ => job)
        }.toList
      }

  }

}
