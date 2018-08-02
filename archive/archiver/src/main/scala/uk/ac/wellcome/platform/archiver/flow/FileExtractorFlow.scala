package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation


object FileExtractorFlow extends Logging {

  def apply(zipFile: ZipFile): Flow[ObjectLocation, ByteString, NotUsed] = {
    Flow[ObjectLocation].flatMapConcat((bagLocation) => {
      Source.single(bagLocation)
        .map(getStream(zipFile, _))
        .flatMapConcat {
          case Some(inputStream) => StreamConverters.fromInputStream(() => inputStream)
          case _ => throw new RuntimeException(s"Failed to get InputStream for $bagLocation")
        }
    })
  }

  private def getStream(zipFile: ZipFile, bagLocation: ObjectLocation) = {
    val name = s"${bagLocation.namespace}/${bagLocation.key}"

    for {
      zipEntry <- Option(zipFile.getEntry(name))
      zipStream <- Option(zipFile.getInputStream(zipEntry))
    } yield zipStream

  }
}
