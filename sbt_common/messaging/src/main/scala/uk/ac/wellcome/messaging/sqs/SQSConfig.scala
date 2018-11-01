package uk.ac.wellcome.messaging.sqs

case class SQSConfig(queueUrl: String,
                     maxMessages: Integer = 10,
                     parallelism: Integer = 10)
