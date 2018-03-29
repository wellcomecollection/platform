package uk.ac.wellcome.platform.snapshot_convertor.flow

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import scala.io.Source.fromFile
import uk.ac.wellcome.test.fixtures.Akka

import scala.sys.process._
import scala.concurrent.{ExecutionContextExecutor, Future}

class StringToGzipFlowTest
    extends FunSpec
    with Matchers
    with Akka
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

      // This code dumps the gzip contents to a gzip file, then unpacks them
      // using a shell command.
      //
      // The intention here isn't to read a gzip-compressed file in the most
      // "Scala-like" way, it's to open the file in a way similar to the
      // way our users are likely to use.
      val tmpfile = File.createTempFile("stringToGzipFlowTest", ".txt.gz")
      val fileSink: Sink[ByteString, Future[IOResult]] = Flow[ByteString]
        .toMat(FileIO.toPath(Paths.get(tmpfile.getPath)))(Keep.right)

      val future: Future[IOResult] = source
        .via(flow)
        .runWith(fileSink)

      whenReady(future) { _ =>
        // Unzip the file, then open it and check it contains the strings
        // we'd expect.  This is the Busybox version of gzip, which strips
        // the .gz from the filename.
        s"gunzip ${tmpfile.getPath}" !!

        val fileContents =
          fromFile(tmpfile.getPath.replace(".txt.gz", ".txt")).mkString

        // Files have a trailing newline
        val expectedContents = expectedLines.mkString("\n") + "\n"

        fileContents shouldBe expectedContents
      }
    }
  }
}
