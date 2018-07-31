package uk.ac.wellcome.platform.archiver.flow

import akka.stream.ActorMaterializer
import akka.{Done, NotUsed}
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source}
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.Future

object DownloadVerificationFlow {
  def apply(checksum: String)(implicit s3Client: S3Client, materializer: ActorMaterializer):
    Flow[ObjectLocation, Future[Done], NotUsed] = {

    val verify = DigestCalculatorFlow("MD5", checksum)

    val download = Flow[ObjectLocation].flatMapConcat((uploadLocation) => {
      s3Client.download(uploadLocation.namespace, uploadLocation.key)._1
    })

    Flow[ObjectLocation].map((uploadLocation) => {
      val (_, downloadResult) = download.via(verify).runWith(Source.single(uploadLocation), Sink.ignore)

      downloadResult
    })
  }
}
