package uk.ac.wellcome.storage.s3

import java.io.ByteArrayInputStream

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class S3StorageTest
    extends FunSpec
    with S3
    with Matchers
    with ScalaFutures
    with ExtendedPatience {

  val content = "red ravens running a royal rolls royce"
  val expectedHash = "1213356621"

  it("puts an object in S3 with a suffix and prefix") {
    withLocalS3Bucket { bucket =>
      val inputStream = new ByteArrayInputStream(content.getBytes)

      val store =
        S3Storage.put(s3Client, bucket.name)("prefix", Some(".extension"))(
          inputStream)

      val expectedLocation =
        S3ObjectLocation(bucket.name, s"prefix/$expectedHash.extension")

      whenReady(store) { location =>
        location shouldBe expectedLocation
        getContentFromS3(location) shouldBe content
      }
    }
  }

  it("puts an object in S3 with a prefix only") {
    withLocalS3Bucket { bucket =>
      val inputStream = new ByteArrayInputStream(content.getBytes)

      val store = S3Storage.put(s3Client, bucket.name)("prefix")(inputStream)

      val expectedLocation =
        S3ObjectLocation(bucket.name, s"prefix/$expectedHash")

      whenReady(store) { location =>
        location shouldBe expectedLocation
        getContentFromS3(location) shouldBe content
      }
    }
  }

  it("gets an object from S3") {
    withLocalS3Bucket { bucket =>
      val key = "my/content"

      s3Client.putObject(bucket.name, "my/content", content)

      val retrieval = S3Storage.get(s3Client, bucket.name)(key)

      whenReady(retrieval) { inputStream =>
        stringify(inputStream) shouldBe content
      }
    }
  }

  it("removes leading slashes from prefixes") {
    withLocalS3Bucket { bucket =>
      val inputStream = new ByteArrayInputStream(content.getBytes)

      val prefix = "/foo"

      val store = S3Storage.put(s3Client, bucket.name)(prefix)(inputStream)
      val expectedLocation =
        S3ObjectLocation(bucket.name, s"foo/$expectedHash")

      whenReady(store) { actualLocation =>
        expectedLocation shouldBe actualLocation
      }

    }
  }

  it("removes trailing slashes from prefixes") {
    withLocalS3Bucket { bucket =>
      val inputStream = new ByteArrayInputStream(content.getBytes)

      val prefix = "foo/"

      val store = S3Storage.put(s3Client, bucket.name)(prefix)(inputStream)
      val expectedLocation =
        S3ObjectLocation(bucket.name, s"foo/$expectedHash")

      whenReady(store) { actualLocation =>
        expectedLocation shouldBe actualLocation
      }
    }
  }

  it("removes trailing dots from suffixes") {
    withLocalS3Bucket { bucket =>
      val inputStream = new ByteArrayInputStream(content.getBytes)

      val suffix = ".json"

      val store = S3Storage.put(s3Client, bucket.name)("prefix", Some(suffix))(
        inputStream)
      val expectedLocation =
        S3ObjectLocation(bucket.name, s"foo/$expectedHash")

      whenReady(store) { actualLocation =>
        expectedLocation shouldBe actualLocation
      }
    }
  }

}
