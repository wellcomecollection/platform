package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Zip}
import akka.util.ByteString
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.storage.ObjectLocation

object VerifiedUploadFlow {
  def apply(s3Client: S3Client, uploadLocation: ObjectLocation): Flow[ArchiveItemJob, (MultipartUploadResult, ByteString), NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit b =>

        import GraphDSL.Implicits._

        val job = b.add(Flow[ArchiveItemJob])
        val extract = b.add(ZipFileEntryFlow())
        val verify = b.add(ArchiveChecksumFlow("SHA-256"))
        val zip = b.add(Zip[MultipartUploadResult, ByteString])
        val upload = b.add(createS3UploadFlow(s3Client, uploadLocation))

        job.map(ZipLocation(_)) ~> extract ~> verify.inlets.head

        verify.outlets(0) ~> upload ~> zip.in0
        verify.outlets(1) ~> zip.in1

        FlowShape(job.in, zip.out)
      }
    )
  }

  private def createS3UploadFlow(s3Client: S3Client, objectLocation: ObjectLocation) = {
    val s3Sink = s3Client.multipartUpload(
      objectLocation.namespace,
      objectLocation.key
    )

    Flow.fromGraph(GraphDSL.create(s3Sink) { implicit builder =>
      sink => FlowShape(sink.in, builder.materializedValue)
    }).flatMapConcat(Source.fromFuture)
  }
}
