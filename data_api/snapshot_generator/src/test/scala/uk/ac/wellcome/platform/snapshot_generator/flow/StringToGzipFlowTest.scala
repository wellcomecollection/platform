package uk.ac.wellcome.platform.snapshot_generator.flow

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.{ExecutionContextExecutor, Future}

class StringToGzipFlowTest
    extends FunSpec
    with Matchers
    with Akka
    with GzipUtils
    with ExtendedPatience
    with ScalaFutures {

  it("produces a gzip-compressed file from the lines") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      implicit val system: ActorSystem = actorSystem
      implicit val materializer: Materializer =
        ActorMaterializer()(actorSystem)

      val flow = StringToGzipFlow()

      val expectedLines = List(
        "Amber avocados age admirably",
        "Bronze bananas barely bounce",
        "Cerise coconuts craftily curl"
      )

      val source = Source(expectedLines)

      // This code dumps the gzip contents to a gzip file.
      val tmpfile = File.createTempFile("stringToGzipFlowTest", ".txt.gz")
      val fileSink: Sink[ByteString, Future[IOResult]] = Flow[ByteString]
        .toMat(FileIO.toPath(Paths.get(tmpfile.getPath)))(Keep.right)

      val future: Future[IOResult] = source
        .via(flow)
        .runWith(fileSink)

      whenReady(future) { _ =>
        // Unzip the file, then open it and check it contains the strings
        // we'd expect.  Note that files have a trailing newline.
        val fileContents = readGzipFile(tmpfile.getPath)
        val expectedContents = expectedLines.mkString("\n") + "\n"

        fileContents shouldBe expectedContents
      }
    }
  }
}
