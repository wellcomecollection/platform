resource "aws_sqs_queue" "q" {
  name   = "${var.queue_name}"
  policy = "${data.aws_iam_policy_document.sqs_queue_policy.json}"
}
