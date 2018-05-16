package uk.ac.wellcome.messaging.sqs

import scala.concurrent.duration.Duration

case class SQSConfig(queueUrl: String,
                     waitTime: Duration,
                     maxMessages: Integer,
                     parallelism: Integer = 10)
