module "reindex_request_creator_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v10.2.3"
  queue_name  = "reindex_request_creator_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.reindex_jobs_topic.name}"]

  # The reindex_request_creator scans a source data table to look for
  # records that need reindexing, and sends each of them as an SNS notification
  # for the reindex_request_processor.
  #
  # If it fails midway through, it rescans and resends *the entire shard* --
  # leading to significantly more work for the processor!  If it's already
  # sent a notification for a record, but the processor hasn't reindexed
  # it yet, it gets resent.
  #
  # To that end, we have a very long visibility timeout on this queue -- so
  # once it's sent a batch of notifications, the processor has a chance to
  # pick them up before the creator sends a fresh batch.
  #
  # (tl;dr: There is a sensible reason for the timeout to be this long, and
  #  we're not just trying to evade flakiness.)
  #
  visibility_timeout_seconds = 600

  max_receive_count = 50
  alarm_topic_arn   = "${local.dlq_alarm_arn}"
}

module "reindex_requests_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v10.2.3"
  queue_name  = "reindex_requests_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.reindex_requests_topic.name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 3
  alarm_topic_arn            = "${local.dlq_alarm_arn}"
}
