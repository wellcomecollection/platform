module "schedule_reindexer" {
  source = "schedule_reindexer"

  dynamodb_table_reindex_tracker_stream_arn = "${aws_dynamodb_table.reindex_tracker.stream_arn}"
  ecs_services_cluster_name                 = "${aws_ecs_cluster.services.name}"
  dynamodb_table_miro_table_name            = "${aws_dynamodb_table.miro_table.name}"

  dynamo_capacity_topic_arn            = "${local.dynamo_capacity_topic_arn}"
  dynamo_capacity_topic_publish_policy = "${local.dynamo_capacity_topic_publish_policy}"

  service_scheduler_topic_arn            = "${local.service_scheduler_topic_arn}"
  service_scheduler_topic_publish_policy = "${local.service_scheduler_topic_publish_policy}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}