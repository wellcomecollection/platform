package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Zip}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult
import uk.ac.wellcome.platform.archive.archivist.models.storage.ObjectMetadata
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object UploadAndCalculateDigestFlow {
  def apply(uploadLocation: ObjectLocation,
            maybeObjectMetadata: Option[ObjectMetadata] = None)(
    implicit s3Client: AmazonS3): Flow[ByteString, Try[String], NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val verify = b.add(ArchiveChecksumFlow("SHA-256"))
        val flow = b.add(Flow[ByteString])
        val upload = b.add(S3UploadFlow(uploadLocation, maybeObjectMetadata))
        val zip = b.add(Zip[Try[CompleteMultipartUploadResult], String])

        flow.out.log("calculating checksum") ~> verify.inlets.head

        verify.out0.log("uploading to s3") ~> upload ~> zip.in0

        verify.out1 ~> zip.in1

        FlowShape(flow.in, zip.out.map {
          case (triedUploadResult, checksum) =>
            triedUploadResult.map(_ => checksum)
        }.outlet)
      }
    )
  }
}
