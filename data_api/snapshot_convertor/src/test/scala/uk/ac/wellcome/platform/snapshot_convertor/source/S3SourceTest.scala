package uk.ac.wellcome.platform.snapshot_convertor.source

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, Sink, Source}
import akka.util.ByteString
import java.io.{BufferedWriter, File, FileWriter}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import scala.sys.process._
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future

class S3SourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with Akka
    with AkkaS3 {

  it("reads a series of lines from S3") {
    withActorSystem { actorSystem =>
      implicit val system = actorSystem
      implicit val materializer = ActorMaterializer()

      withLocalS3Bucket { bucketName =>
        withS3AkkaClient(actorSystem, materializer) { akkaS3client =>
          val expectedLines = List(
            "AARGH! An armada of alpaccas",
            "Blimey! Blue bonobos with beachballs",
            "Cor! Countless copies of crying camels"
          )

          val gzipFile = createGzipFile(expectedLines.mkString("\n"))
          val key = "test001.txt.gz"
          s3Client.putObject(bucketName, key, gzipFile)

          val source = S3Source(
            s3client = akkaS3client,
            bucketName = bucketName,
            key = key
          )

          val future = source.runWith(Sink.seq)
          whenReady(future) { result =>
            result.toList shouldBe expectedLines
          }
        }
      }
    }
  }

  private def createGzipFile(content: String): File = {
    val tmpfile = File.createTempFile("s3sourcetest", ".txt")

    // Create a gzip-compressed file.  This is based on the shell commands
    // that are used in the elasticdump container.
    //
    // The intention here is not to create a gzip-compressed file in
    // the most "Scala-like" way, it's to create a file that closely
    // matches what we'd get from an elasticdump in S3.
    val bw = new BufferedWriter(new FileWriter(tmpfile))
    bw.write(content)
    bw.close()

    // This performs "gzip foo.txt > foo.txt.gz" in a roundabout way.
    // Because we're using the busybox version of gzip, it actually cleans
    // up the old file and appends the .gz suffix for us.
    s"gzip ${tmpfile.getPath}" !!

    new File(s"${tmpfile.getPath}.gz")
  }
}
