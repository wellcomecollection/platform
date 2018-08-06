package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveItemFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagDigestItem, ZipFile), Done, NotUsed] = {

    val uploadVerificationFlow
      : Flow[(BagDigestItem, ZipFile), MultipartUploadResult, NotUsed] =
      UploadVerificationFlow(config)

    val uploadLocationFlow
      : Flow[MultipartUploadResult, ObjectLocation, NotUsed] =
      Flow[MultipartUploadResult]
        .map {
          case MultipartUploadResult(_, bucket, key, _) =>
            ObjectLocation(bucket, key)
        }
        .log("upload location")

    val downloadVerification = DownloadVerificationFlow()

    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val objectLocationFlow = b.add(Flow[(BagDigestItem, ZipFile)])
      val uploadVerificationFlowShape
        : FlowShape[(BagDigestItem, ZipFile), MultipartUploadResult] =
        b.add(uploadVerificationFlow)
      val uploadLocationFlowShape = b.add(uploadLocationFlow)
      val downloadVerificationZipIn = b.add(Zip[ObjectLocation, String])
      val downloadVerificationFlowShape = b.add(downloadVerification)

      val broadcast = b.add(Broadcast[(BagDigestItem, ZipFile)](2))

      objectLocationFlow ~> broadcast.in

      broadcast.out(0) ~> uploadVerificationFlowShape ~> uploadLocationFlowShape

      uploadLocationFlowShape ~> downloadVerificationZipIn.in0

      broadcast.out(1).map {
        case (bagDigestItem, _) => bagDigestItem.checksum
      } ~> downloadVerificationZipIn.in1

      downloadVerificationZipIn.out ~> downloadVerificationFlowShape

      FlowShape(objectLocationFlow.in, downloadVerificationFlowShape.out)
    })
  }
}
