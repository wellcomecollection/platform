package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File

import akka.stream.FlowShape
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Source}

object FileStoreFlow {
  def apply(tmpFile: File, parallelism: Int) = {
    val fileSink = FileIO.toPath(tmpFile.toPath)

    Flow
      .fromGraph(GraphDSL.create(fileSink) { implicit builder => sink =>
        FlowShape(sink.in, builder.materializedValue)
      })
      .flatMapMerge(parallelism, (Source.fromFuture))

  }
}
