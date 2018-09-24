package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.storage.ObjectLocation

object UploadAndGetChecksumFlow {
  def apply(uploadLocation: ObjectLocation)(implicit s3Client: AmazonS3): Flow[ByteString, ByteString, NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit b =>

        import GraphDSL.Implicits._

        val verify = b.add(ArchiveChecksumFlow("SHA-256"))
        val broadcast = b.add(Flow[ByteString])
        val upload = b.add(S3UploadFlow(uploadLocation))
        val ignore = b.add(Sink.ignore)

        broadcast.out ~> verify.inlets.head

        verify.outlets(0).log("uploading to s3") ~> upload ~> ignore.in

        FlowShape(broadcast.in, verify.outlets(1))
      }
    )
  }

}
