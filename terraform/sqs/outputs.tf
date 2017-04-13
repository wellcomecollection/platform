output "id" {
  description = "URL for the created SQS queue"
  value       = "${aws_sqs_queue.q.id}"
}

output "q_arn" {
  description = "ARN for the created SQS queue"
  value       = "${aws_sqs_queue.q.arn}"
}
