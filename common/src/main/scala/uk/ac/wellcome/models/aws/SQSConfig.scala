package uk.ac.wellcome.models.aws

import scala.concurrent.duration.Duration

case class SQSConfig(queueUrl: String,
                     waitTime: Duration,
                     maxMessages: Integer)
