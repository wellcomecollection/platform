resource "aws_sns_topic_subscription" "sns_topic" {
  count     = "${var.topic_count}"
  protocol  = "sqs"
  topic_arn = "${format("arn:aws:sns:%s:%s:%s", var.aws_region, var.account_id, element(var.topic_names, count.index))}"
  endpoint  = "${aws_sqs_queue.q.arn}"
}
