package uk.ac.wellcome.platform.archive.archivist.models.storage

import org.scalatest.{FunSpec, Matchers}
import com.amazonaws.services.s3.model.{ObjectMetadata => S3ObjectMetadata}

class ObjectMetadataTest extends FunSpec with Matchers {
  it("can be converted to S3ObjectMetadata") {
    val objectMetadata = ObjectMetadata(Map("key" -> "value"))

    val expectedMetadata = new S3ObjectMetadata()
    expectedMetadata.addUserMetadata("key", "value")

    objectMetadata.toS3ObjectMetadata.getUserMetadata shouldBe expectedMetadata.getUserMetadata
  }
}
