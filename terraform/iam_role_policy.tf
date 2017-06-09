# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_read_ingestor_q" {
  name   = "ecs_task_ingestor_policy"
  role   = "${module.ecs_ingestor_iam.task_role_name}"
  policy = "${module.es_ingest_queue.read_policy}"
}

resource "aws_iam_role_policy" "ecs_ingestor_task_cloudwatch_metric" {
  name   = "ecs_task_cloudwatch_metric_policy"
  role   = "${module.ecs_ingestor_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the Calm adapter

resource "aws_iam_role_policy" "ecs_calm_adapter_task" {
  name   = "ecs_task_calm_adapter_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_calm_db_all.json}"
}

resource "aws_iam_role_policy" "ecs_calm_adapter_service_scheduler_sns" {
  name   = "ecs_task_calm_service_scheduler_sns_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_dynamo" {
  name   = "ecs_task_transformer_task_dynamo_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_dynamodb_all.json}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${module.id_minter_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_kinesis_stream" {
  name   = "ecs_task_kinesis_stream_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_calm_kinesis_stream.json}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_cloudwatch_metric" {
  name   = "ecs_task_cloudwatch_metric_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the ID minter

resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${module.es_ingest_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_id_minter_task_read_id_minter_q" {
  name   = "ecs_task_id_minter_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${module.id_minter_queue.read_policy}"
}

# Role policies for the Publish to SNS Lambda

resource "aws_iam_role_policy" "lambda_service_scheduler_sns" {
  name   = "lambda_service_scheduler_sns_policy"
  role   = "${module.lambda_service_scheduler.role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_schedule_reindexer_sns" {
  name   = "lambda_schedule_reindexer_sns"
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_schedule_reindexer_dynamo_sns" {
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${module.dynamo_capacity_topic.publish_policy}"
}

# Role policies for the Update ECS Service Size Lambda

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name   = "lambda_update_ecs_service_size"
  role   = "${module.lambda_update_ecs_service_size.role_name}"
  policy = "${data.aws_iam_policy_document.update_ecs_service_size.json}"
}

# Role policies for the Lambda which updates running tasks when the
# config changes.

resource "aws_iam_role_policy" "update_tasks_for_config_change_policy" {
  role   = "${module.lambda_update_task_for_config_change.role_name}"
  policy = "${data.aws_iam_policy_document.stop_running_tasks.json}"
}

resource "aws_iam_role_policy" "reindexer_tracker_table" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_tracker_table.json}"
}

resource "aws_iam_role_policy" "reindexer_target_miro" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_target_miro.json}"
}

resource "aws_iam_role_policy" "reindexer_cloudwatch" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "id_minter_cloudwatch" {
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
