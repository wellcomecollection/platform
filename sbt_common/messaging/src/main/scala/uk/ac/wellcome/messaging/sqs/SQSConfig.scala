package uk.ac.wellcome.messaging.sqs

import scala.concurrent.duration._

case class SQSConfig(queueUrl: String,
                     waitTime: Duration = 10 seconds,
                     maxMessages: Integer = 10,
                     parallelism: Integer = 10)
