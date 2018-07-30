package uk.ac.wellcome.platform.archiver.flow

import java.io.InputStream
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import akka.util.ByteString
import uk.ac.wellcome.platform.archiver.DigestCalculator
import uk.ac.wellcome.storage.ObjectLocation

object UploadVerificationFlow {
  def apply(zipFile: ZipFile, uploadLocation: ObjectLocation, checksum: String)(implicit s3Client: S3Client, materializer: ActorMaterializer) = {

    val verify: DigestCalculator = new DigestCalculator("MD5", checksum)

    val upload = s3Client.multipartUpload(uploadLocation.namespace, uploadLocation.key)

    val extract: Flow[ObjectLocation, ByteString, NotUsed] = Flow[ObjectLocation].flatMapConcat((bagLocation) => {
      StreamConverters.fromInputStream(() => {
        getStream(zipFile, bagLocation)
      })
    })

    Flow[ObjectLocation].map((bagLocation) => {
      val (_, uploadResult) = extract.via(verify).runWith(Source.single(bagLocation), upload)

      uploadResult
    })
  }

  private def getStream(zipFile: ZipFile, bagLocation: ObjectLocation): InputStream =
    zipFile.getInputStream(
      zipFile.getEntry(s"${bagLocation.namespace}/${bagLocation.key}")
    )
}
