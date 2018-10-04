package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.util.ByteString

object VerifiedDownloadFlow {
  def apply(): Flow[ByteString, String, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      {

        import GraphDSL.Implicits._

        val v = b.add(ArchiveChecksumFlow("SHA-256"))
        val ignore = b.add(Sink.ignore)

        v.out0 ~> ignore.in

        FlowShape(v.in, v.out1)
      }
    })
}
