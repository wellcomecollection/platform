resource "aws_sqs_queue" "q" {
  name           = "${var.queue_name}"
  policy         = "${data.aws_iam_policy_document.write_to_queue.json}"
  redrive_policy = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.dlq.arn}\",\"maxReceiveCount\":${var.max_receive_count}}"
}

resource "aws_sqs_queue" "dlq" {
  name = "${var.queue_name}_dlq"
}
