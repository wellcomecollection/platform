module "reindex_worker_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v10.2.3"
  queue_name  = "reindex_worker_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.reindex_jobs_topic.name}"]

  # Keep messages from returning to the queue
  # while being processed.
  visibility_timeout_seconds = 600

  max_receive_count = 3
  alarm_topic_arn   = "${local.dlq_alarm_arn}"
}
