module "windows_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_windows_queue_${var.resource_type}"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${var.windows_topic_arn}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
