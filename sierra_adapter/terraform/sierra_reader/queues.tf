module "windows_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v6.4.0"
  queue_name  = "sierra_${var.resource_type}_windows"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.windows_topic_name}"]

  # Ensure that messages are spread around -- if we get a timeout from the
  # Sierra API, we don't retry _too_ quickly.
  visibility_timeout_seconds = 90

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
