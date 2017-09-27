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

# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_read_transformer_q" {
  name   = "ecs_transformer_task_read_transformer_q"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${module.miro_transformer_queue.read_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${module.id_minter_topic.publish_policy}"
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

resource "aws_iam_role_policy" "id_minter_cloudwatch" {
  role   = "${module.ecs_id_minter_iam.task_role_name}"
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

# grafana policies

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "ecs_grafana_task_cloudwatch_read"

  # Unfortunately grafana seems to assume the role of the ec2 instance the container is running into.
  # This used to be a bug in grafana which was fixed in version 4.3.0: https://github.com/grafana/grafana/pull/7892
  # Unfortunately we are still seeing this behaviour from the official grafana docker image
  # TODO change to role = "${module.ecs_grafana_iam.task_role_name}"
  role = "${module.ecs_monitoring_iam.instance_role_name}"

  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

# Policies for the TIF conversion batch job

resource "aws_iam_role_policy" "batch_tif_conversion_s3_tif_derivative" {
  role   = "${module.batch_tif_conversion_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_tif_derivative.json}"
}

# Policies for the Elasticdump task

resource "aws_iam_role_policy" "elasticdump_read_ingestor_config_from_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_read_ingestor_config.json}"
}

resource "aws_iam_role_policy" "elasticdump_upload_files_to_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_upload_to_to_elasticdump_directory.json}"
}
