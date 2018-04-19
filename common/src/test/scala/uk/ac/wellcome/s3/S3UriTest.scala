package uk.ac.wellcome.s3

import org.scalatest._
import java.net.URI

class S3UriTest extends FunSpec with Matchers with Inside {

  val s3Uri = new URI("s3://bucket/key")

  it("can be created from a bucket and key") {
    S3Uri("bucket", "key") shouldBe new URI("s3://bucket/key")
  }

  it("matches S3 URI bucket name") {
    inside(s3Uri) {
      case S3Uri(bucket, _) => bucket should be("bucket")
    }
  }

  it("matches S3 URI object key") {
    inside(s3Uri) {
      case S3Uri(_, key) => key should be("key")
    }
  }

  it("does not match non-S3 URI") {
    new URI("http://bucket/key") match {
      case S3Uri(_, _) => fail()
      case _ => ()
    }
  }

}
