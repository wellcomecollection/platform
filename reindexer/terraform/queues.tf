module "reindexer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v10.2.3"
  queue_name  = "reindexer_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.reindex_jobs_topic.name}"]

  # Each reindexer message requires up to 1500 calls to DynamoDB's PutItem API
  # (one for every record in the shard).  Particularly when the reindexer has
  # just started, it tends to hit the throughput limits, and the message
  # hits the DLQ.
  #
  # The long-term fix is to rearchitect the reindexer to behave in a better
  # way, but for now we have this short-term fix:
  #
  #   - Wait 2 minutes before a message can be reprocessed.  If it hits limits
  #     on the first attempt, the table should have warmed up before it comes
  #     round again.
  #   - Retry 50 times per job, so we have to average 30 successful PutItem
  #     calls per attempt.  This seems more feasible than the default.
  #
  # TODO: Reduce these limits when we fix the reindexer.
  #
  visibility_timeout_seconds = 120

  max_receive_count = 50

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}
