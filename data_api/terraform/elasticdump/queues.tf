module "elasticdump_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "elasticdump_schedule_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.miro_transformer_topic.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
