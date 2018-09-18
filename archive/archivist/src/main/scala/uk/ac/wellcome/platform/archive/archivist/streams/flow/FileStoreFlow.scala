package uk.ac.wellcome.platform.archive.archivist.streams.flow

import java.io.File

import akka.stream.FlowShape
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Source}

object FileStoreFlow {
  def apply(tmpFile: File) = {
    val fileSink = FileIO.toPath(tmpFile.toPath)

    Flow.fromGraph(GraphDSL.create(fileSink) { implicit builder =>
      sink =>
        FlowShape(sink.in, builder.materializedValue)
    }).flatMapConcat(Source.fromFuture)
  }
}
