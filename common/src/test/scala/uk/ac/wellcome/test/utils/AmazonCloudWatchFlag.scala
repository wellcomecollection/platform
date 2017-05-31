package uk.ac.wellcome.test.utils

import org.scalatest.Suite

trait AmazonCloudWatchFlag { this: Suite =>
  val cloudWatchLocalEndpointFlag: Map[String, String] =
    Map(
      // use a fake endpoint in tests so that we don't send metrics to the real AWS
      "aws.cloudWatch.endpoint" -> "http://localhost:6789")
}
