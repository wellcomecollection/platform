# Role policies for the miro transformer

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.miro_transformer.task_role_name}"
  policy = "${module.ingest_pipeline_mel.id_minter_topic_publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_cloudwatch_metric" {
  role   = "${module.miro_transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the sierra transformer

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.sierra_transformer.task_role_name}"
  policy = "${module.ingest_pipeline_sue.id_minter_topic_publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_cloudwatch_metric" {
  role   = "${module.sierra_transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the miro_reindexer service

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

# Policies for the Elasticdump task

resource "aws_iam_role_policy" "elasticdump_read_ingestor_config_from_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${module.ingest_pipeline_mel.read_ingestor_config_policy_document}"
}

resource "aws_iam_role_policy" "elasticdump_upload_files_to_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_upload_to_to_elasticdump_directory.json}"
}

# Policies for the schedule_reindexer task

resource "aws_iam_role_policy" "lambda_schedule_reindexer_sns" {
  name   = "lambda_schedule_reindexer_sns"
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${local.service_scheduler_topic_publish_policy}"
}

resource "aws_iam_role_policy" "lambda_schedule_reindexer_dynamo_sns" {
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${local.dynamo_capacity_topic_publish_policy}"
}
