resource "aws_sns_topic_subscription" "sns_topic" {
  protocol  = "sqs"
  topic_arn = "${var.topic_arn}"
  endpoint  = "${aws_sqs_queue.q.arn}"
}
