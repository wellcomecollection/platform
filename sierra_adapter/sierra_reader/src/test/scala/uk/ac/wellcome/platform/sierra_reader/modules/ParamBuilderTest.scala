package uk.ac.wellcome.platform.sierra_reader.modules

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes
import uk.ac.wellcome.sqs.SQSReaderGracefulException
import uk.ac.wellcome.test.utils.{ExtendedPatience, S3Local}

class ParamBuilderTest extends FunSpec with Matchers with S3Local with ScalaFutures with ExtendedPatience {
  val bucketName: String = createBucketAndReturnName("param-builder-test-bucket")

  it("returns the standard params if there isn't a window in progress") {
    val paramBuilder = new ParamBuilder(
      s3client = s3Client,
      bucketName = bucketName,
      fields = "title",
      resourceType = SierraResourceTypes.bibs
    )

    val result = paramBuilder.buildParams("[2013,2014]")

    whenReady(result) { _ shouldBe Map("updatedDate" -> "[2013,2014]", "fields" -> "title")}
  }

  it("adds the ID to the standard params if there is a window in progress") {
    val paramBuilder = new ParamBuilder(
      s3client = s3Client,
      bucketName = bucketName,
      fields = "title",
      resourceType = SierraResourceTypes.bibs
    )

    val prefix = paramBuilder.buildWindowShard("[2013,2014]")

    // We pre-populate S3 with files as if they'd come from a prior run of the reader.
    s3Client.putObject(bucketName, s"$prefix/0000.json", "[]")
    s3Client.putObject(bucketName, s"$prefix/0001.json",
      """
        |[
        |  {
        |    "id": "1794165",
        |    "modifiedDate": 12345678,
        |    "data": "{}"
        |  }
        |]
      """.stripMargin)

    val result = paramBuilder.buildParams("[2013,2014]")

    whenReady(result) { _ shouldBe Map(
      "updatedDate" -> "[2013,2014]",
      "fields" -> "title",
      "id" -> "1794166"
    )}
  }

  it("throws an error if it finds invalid JSON in the bucket") {
    val paramBuilder = new ParamBuilder(
      s3client = s3Client,
      bucketName = bucketName,
      fields = "title",
      resourceType = SierraResourceTypes.bibs
    )

    val prefix = paramBuilder.buildWindowShard("[2013,2014]")

    s3Client.putObject(bucketName, s"$prefix/0000.json", "not valid")

    val result = paramBuilder.buildParams("[2013,2014]")

    whenReady(result.failed) {
      _ shouldBe a[SQSReaderGracefulException]
    }
  }

  it("throws an error if it finds empty JSON in the bucket") {
    val paramBuilder = new ParamBuilder(
      s3client = s3Client,
      bucketName = bucketName,
      fields = "title",
      resourceType = SierraResourceTypes.bibs
    )

    val prefix = paramBuilder.buildWindowShard("[2013,2014]")

    s3Client.putObject(bucketName, s"$prefix/0000.json", "[]")

    val result = paramBuilder.buildParams("[2013,2014]")

    whenReady(result.failed) {
      _ shouldBe a[SQSReaderGracefulException]
    } }
}
