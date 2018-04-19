module "snapshot_generator_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v8.0.2"
  queue_name  = "snapshot_generator_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.snapshot_scheduler.topic_name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
  visibility_timeout_seconds = 1800
}
