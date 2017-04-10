package uk.ac.wellcome.models.aws

import scala.concurrent.duration.Duration

case class SQSConfig(region: String, queueUrl: String,
                     waitTime: Duration,
                     maxMessages: Integer)
