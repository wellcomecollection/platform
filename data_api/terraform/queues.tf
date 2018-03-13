module "snapshot_convertor_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v1.1.0"
  queue_name  = "snapshot_convertor_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.snapshot_convertor_topic.name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}
