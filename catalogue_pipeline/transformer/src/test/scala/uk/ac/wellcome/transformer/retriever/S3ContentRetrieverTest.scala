package uk.ac.wellcome.transformer.retriever

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.test.utils.S3Local

class S3ContentRetrieverTest
    extends FunSpec
    with Matchers
    with S3Local
    with ScalaFutures {

  override lazy val bucketName = "hybrid-record-parser-test"
  it("receives a s3 key and returns the content of the file") {
    val s3key = "sierra/1/b1111.json"
    val content = "this is the content of the s3 file"
    s3Client.putObject(bucketName, s3key, content)

    val s3ContentRetriever = new S3ContentRetriever(s3Client, bucketName)

    whenReady(s3ContentRetriever.getS3Content(s3key)) { actualContent =>
      actualContent shouldBe content
    }
  }

  it(
    "fails with GracefulFailureException if the key does not exist in the bucket") {
    val s3key = "this/key/does/not/exist.json"

    val s3ContentRetriever = new S3ContentRetriever(s3Client, bucketName)

    whenReady(s3ContentRetriever.getS3Content(s3key).failed) { ex =>
      ex shouldBe a[GracefulFailureException]
    }
  }
}
