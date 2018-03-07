module "reindex_shard_tracker_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns"

  name           = "reindex_shard_tracker_updates"
  src_stream_arn = "${aws_dynamodb_table.reindex_shard_tracker.stream_arn}"
  dst_topic_arn  = "${module.reindex_shard_tracker_topic.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  infra_bucket = "${var.infra_bucket}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
