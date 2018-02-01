# Role policies for the miro transformer

resource "aws_iam_role_policy" "miro_ecs_transformer_task_sns" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.ingest_pipeline_mel.id_minter_topic_publish_policy}"
}

resource "aws_iam_role_policy" "miro_ecs_transformer_task_cloudwatch_metric" {
  role   = "${module.transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the unified transformer

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.ingest_pipeline_sue.id_minter_topic_publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.versioned-hybrid-store.read_policy}"
}

resource "aws_iam_role_policy" "sierra_transformer_task_cloudwatch_metric" {
  role   = "${module.transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the miro_reindexer service

resource "aws_iam_role_policy" "reindexer_tracker_table" {
  role   = "${module.miro_reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_tracker_table.json}"
}

resource "aws_iam_role_policy" "reindexer_target_miro" {
  role   = "${module.miro_reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_target_miro.json}"
}

resource "aws_iam_role_policy" "reindexer_cloudwatch" {
  role   = "${module.miro_reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Policies for the schedule_reindexer task

resource "aws_iam_role_policy" "lambda_schedule_reindexer_sns" {
  name   = "lambda_schedule_reindexer_sns"
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${local.service_scheduler_topic_publish_policy}"
}
