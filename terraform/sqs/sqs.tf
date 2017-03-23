resource "aws_sqs_queue" "q" {
  name           = "${var.queue_name}"
  policy         = "${data.aws_iam_policy_document.sqs_queue_policy.json}"
  redrive_policy = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.dlq.arn}\",\"maxReceiveCount\":${var.max_recieve_count}}"
}

resource "aws_sqs_queue" "dlq" {
  name = "${var.queue_name}_dlq"
}
