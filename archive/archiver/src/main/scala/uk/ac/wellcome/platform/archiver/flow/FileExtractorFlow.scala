package uk.ac.wellcome.platform.archiver.flow

import java.io.InputStream
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, StreamConverters}
import akka.util.ByteString
import uk.ac.wellcome.storage.ObjectLocation


object FileExtractorFlow {

  def apply(zipFile: ZipFile): Flow[ObjectLocation, ByteString, NotUsed] = {
    Flow[ObjectLocation].flatMapConcat((bagLocation) => {
      StreamConverters.fromInputStream(() => {
        getStream(zipFile, bagLocation)
      })
    })
  }

  private def getStream(zipFile: ZipFile, bagLocation: ObjectLocation): InputStream = {
    zipFile.getInputStream(
      zipFile.getEntry(s"${bagLocation.namespace}/${bagLocation.key}")
    )
  }
}
