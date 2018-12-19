package uk.ac.wellcome.platform.archive.common

import org.scalatest.FunSpec
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3

import scala.util.{Failure, Success}

class ConvertibleToInputStreamTest extends FunSpec with S3 with RandomThings {

  implicit val implicitS3Client = s3Client

  import ConvertibleToInputStream._

  describe("converts to a Try[InputStream]") {
    it("produces a failure from an invalid ObjectLocation") {
      ObjectLocation("invalid_bucket", "invalid.key").toInputStream shouldBe a[
        Failure[_]]
    }

    it("produces a success from an valid ObjectLocation") {
      withLocalS3Bucket { bucket =>
        val key = randomAlphanumeric()
        val content = randomAlphanumeric()

        s3Client.putObject(bucket.name, key, content)

        val testInputStream = ObjectLocation(bucket.name, key).toInputStream

        testInputStream shouldBe a[Success[_]]

        scala.io.Source
          .fromInputStream(
            testInputStream.get
          )
          .mkString shouldEqual content
      }
    }
  }
}
