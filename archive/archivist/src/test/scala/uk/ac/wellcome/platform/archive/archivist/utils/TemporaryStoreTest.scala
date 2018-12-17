package uk.ac.wellcome.platform.archive.archivist.utils

import org.scalatest.FunSpec
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3

import scala.io.Source
import scala.util.{Failure, Success}

class TemporaryStoreTest
  extends FunSpec
    with S3
    with RandomThings {

  import TemporaryStore._

  implicit val implicitS3Client = s3Client

  describe("stores a file from an ObjectLocation") {
    describe("with a valid ObjectLocation") {
      it("stores that file and returns a Success[File]") {
        withLocalS3Bucket { bucket =>
          val key = randomAlphanumeric()
          val content = randomAlphanumeric()

          s3Client.putObject(bucket.name, key, content)

          val testTempFile = ObjectLocation(bucket.name, key).downloadTempFile

          testTempFile shouldBe a[Success[_]]

          val fileContents = Source.fromFile(testTempFile.get).getLines.mkString
          fileContents shouldBe (content)
        }
      }
    }

    describe("with an invalid ObjectLocation") {
      it("returns a Failure[_]") {
        val key = randomAlphanumeric()

        val testTempFile = ObjectLocation("invalid_bucket", key).downloadTempFile

        testTempFile shouldBe a[Failure[_]]
      }
    }
  }
}
