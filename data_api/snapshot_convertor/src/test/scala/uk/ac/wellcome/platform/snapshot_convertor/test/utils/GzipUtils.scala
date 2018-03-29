package uk.ac.wellcome.platform.snapshot_convertor.test.utils

import java.io.{BufferedWriter, File, FileWriter}
import scala.sys.process._
import scala.util.Random

import org.scalatest.Assertion

import uk.ac.wellcome.test.fixtures.{S3, TestWith}

trait GzipUtils with S3 {
  def withGzipCompressedS3Key(bucketName: String, content: String)(
    testWith: TestWith[String, Assertion]) = {
    val gzipContent = createGzipFile(content)
    val key = (Random.alphanumeric take 10 mkString).toLowerCase
    s3Client.putObject(bucketName, key, gzipFile)

    testWith(key)
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
