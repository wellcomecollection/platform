package uk.ac.wellcome.platform.snapshot_convertor.test.utils

import java.io.{BufferedWriter, File, FileWriter}
import scala.io.Source.fromFile
import scala.sys.process._
import scala.util.Random

import org.scalatest.Assertion

import uk.ac.wellcome.test.fixtures.{S3, TestWith}

trait GzipUtils extends S3 {
  def withGzipCompressedS3Key(bucket: S3.Bucket, content: String)(
    testWith: TestWith[String, Assertion]) = {
    val gzipFile = createGzipFile(content)
    val key = (Random.alphanumeric take 10 mkString).toLowerCase

    s3Client.putObject(bucket.name, key, gzipFile)

    testWith(key)
  }

  def readGzipFile(path: String): String = {
    // The intention here isn't to read a gzip-compressed file in the most
    // "Scala-like" way, it's to open the file in a way similar to the
    // way our users are likely to use.
    s"gunzip $path" !!

    fromFile(path.replace(".gz", "")).mkString
  }

  def createGzipFile(content: String): File = {
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
    //
    // We set the gzip compression level to the lowest possible (-1),
    // so we can use smaller data sets in tests (and thus use less memory).
    s"gzip -1 ${tmpfile.getPath}" !!

    new File(s"${tmpfile.getPath}.gz")
  }
}
