module "transformer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_transformer_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = "${var.adapter_topic_names}"
  topic_count = "${var.adapter_topic_count}"

  visibility_timeout_seconds = 30
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
