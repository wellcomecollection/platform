package uk.ac.wellcome.platform.snapshot_convertor.test.utils

import uk.ac.wellcome.test.fixtures.S3

import scala.io.Source.fromFile
import scala.sys.process._

trait GzipUtils extends S3 {

  def readGzipFile(path: String): String = {
    // The intention here isn't to read a gzip-compressed file in the most
    // "Scala-like" way, it's to open the file in a way similar to the
    // way our users are likely to use.
    s"gunzip $path" !!

    fromFile(path.replace(".gz", "")).mkString
  }
}
