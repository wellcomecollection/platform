package uk.ac.wellcome.platform.archive.archivist.flow

import akka.Done
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Zip}
import akka.util.ByteString

object VerifiedDownloadFlow {
  def apply() = Flow.fromGraph(
    GraphDSL.create() {
      implicit b => {

        import GraphDSL.Implicits._

        val v = b.add(ArchiveChecksumFlow("SHA-256"))
        val zip = b.add(Zip[Done, ByteString])

        v.inlets.head

        v.outlets(0).fold(Done)((out, _) => out) ~> zip.in0
        v.outlets(1) ~> zip.in1

        FlowShape(v.in, zip.out)
      }
    })
}
