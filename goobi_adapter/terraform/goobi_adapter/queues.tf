module "goobi_items_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v8.0.0"
  queue_name  = "${var.goobi_items_queue_name}"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.goobi_items_topic}"]

  visibility_timeout_seconds = 300

  max_receive_count = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
