package uk.ac.wellcome.platform.archive.common

import java.io.File

import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object TemporaryStore extends Logging {
  val tmpFilePrefix = "wellcome-tmp-"
  val tmpFileSuffix = ".tmp"

  import uk.ac.wellcome.platform.archive.common.ConvertibleToInputStream._

  implicit class TemporaryStoreOps(location: ObjectLocation) {
    def downloadTempFile(implicit s3Client: AmazonS3): Try[File] = {
      location.toInputStream.flatMap { inputStream =>
        val tmpFile = File.createTempFile(
          tmpFilePrefix,
          tmpFileSuffix
        )

        val outputFile = new java.io.FileOutputStream(tmpFile)
        val outputStream = new java.io.PrintStream(outputFile)

        info(s"Storing $location @ ${tmpFile.getAbsolutePath}")

        tmpFile.deleteOnExit()

        Try(
          Iterator
            .continually(inputStream.read)
            .takeWhile(-1 !=)
            .foreach(outputStream.write)
        ).map(_ => tmpFile)
      }
    }
  }
}
