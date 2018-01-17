package uk.ac.wellcome.platform.sierra_reader.modules

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes
import uk.ac.wellcome.test.utils.{ExtendedPatience, S3Local}

class WindowManagerTest
    extends FunSpec
    with Matchers
    with S3Local
    with ScalaFutures
    with ExtendedPatience {
  val bucketName: String = createBucketAndReturnName(
    "window-manager-test-bucket")

  val windowManager = new WindowManager(
    s3client = s3Client,
    bucketName = bucketName,
    fields = "title",
    resourceType = SierraResourceTypes.bibs
  )

  it("returns an empty ID and offset 0 if there isn't a window in progress") {
    val result = windowManager.getCurrentStatus("[2013,2014]")

    whenReady(result) { _ shouldBe WindowStatus(id = None, offset = 0) }
  }

  it("finds the ID if there is a window in progress") {
    val prefix = windowManager.buildWindowShard("[2013,2014]")

    // We pre-populate S3 with files as if they'd come from a prior run of the reader.
    s3Client.putObject(bucketName, s"$prefix/0000.json", "[]")
    s3Client.putObject(
      bucketName,
      s"$prefix/0001.json",
      """
        |[
        |  {
        |    "id": "b1794165",
        |    "modifiedDate": 12345678,
        |    "data": "{}"
        |  }
        |]
      """.stripMargin
    )

    val result = windowManager.getCurrentStatus("[2013,2014]")

    whenReady(result) {
      _ shouldBe WindowStatus(id = Some("1794166"), offset = 2)
    }
  }

  it("throws an error if it finds invalid JSON in the bucket") {
    val prefix = windowManager.buildWindowShard("[2013,2014]")

    s3Client.putObject(bucketName, s"$prefix/0000.json", "not valid")

    val result = windowManager.getCurrentStatus("[2013,2014]")

    whenReady(result.failed) {
      _ shouldBe a[GracefulFailureException]
    }
  }

  it("throws an error if it finds empty JSON in the bucket") {
    val prefix = windowManager.buildWindowShard("[2013,2014]")

    s3Client.putObject(bucketName, s"$prefix/0000.json", "[]")

    val result = windowManager.getCurrentStatus("[2013,2014]")

    whenReady(result.failed) {
      _ shouldBe a[GracefulFailureException]
    }
  }

  it("throws an error if it finds a misnamed file in the bucket") {
    val prefix = windowManager.buildWindowShard("[2013,2014]")
    s3Client.putObject(bucketName, s"$prefix/000x.json", "[]")

    val result = windowManager.getCurrentStatus("[2013,2014]")

    whenReady(result.failed) {
      _ shouldBe a[GracefulFailureException]
    }
  }
}
