package uk.ac.wellcome.messaging.test.fixtures

import org.scalatest.Suite

trait CloudWatch { this: Suite =>
  def cloudWatchLocalFlags =
    Map(
      "aws.cloudWatch.endpoint" -> "http://localhost:6789"
    )
}
