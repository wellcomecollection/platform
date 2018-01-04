module "windows_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_${var.resource_type}_windows"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.windows_topic.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "updates_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_${var.resource_type}_to_dynamo_updates"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.sierra_to_dynamo_updates_topic.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
