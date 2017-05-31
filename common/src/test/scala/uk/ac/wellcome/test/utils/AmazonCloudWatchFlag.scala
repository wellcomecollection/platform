package uk.ac.wellcome.test.utils

import org.scalatest.Suite

trait AmazonCloudWatchFlag extends TestFlagsProvider { this: Suite =>
  override def testFlags: Map[String, String] =
    Map(
      // use a fake endpoint in tests so that we don't send metrics to the real AWS
      "aws.cloudWatch.endpoint" -> "http://localhost:6789") ++ super.testFlags
}
