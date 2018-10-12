package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File

import akka.stream.{FlowShape, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Source}
import akka.util.ByteString

import scala.concurrent.Future

/** Takes a File and a source of bytes, and writes the bytes to the file.
  *
  * It emits the IOResult from the file write.
  *
  */
object FileStoreFlow {
  def apply(tmpFile: File,
            parallelism: Int): Flow[ByteString, IOResult, Future[IOResult]] = {
    val fileSink = FileIO.toPath(tmpFile.toPath)

    Flow
      .fromGraph(GraphDSL.create(fileSink) { implicit builder => sink =>
        FlowShape(sink.in, builder.materializedValue)
      })
      .flatMapMerge(parallelism, Source.fromFuture)
  }
}
