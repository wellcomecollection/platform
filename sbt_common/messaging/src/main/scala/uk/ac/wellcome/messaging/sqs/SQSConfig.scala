package uk.ac.wellcome.messaging.sqs

case class SQSConfig(queueUrl: String, parallelism: Integer = 10)
