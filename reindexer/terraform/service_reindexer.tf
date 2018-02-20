module "reindexer" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v6.4.0"
  name   = "reindexer"

  source_queue_name = "${module.reindexer_queue.name}"
  source_queue_arn  = "${module.reindexer_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_reindex_worker.repository_url}"
  release_id         = "${var.release_ids["reindex_worker"]}"

  cpu    = 512
  memory = 2048

  env_vars = {
    dynamo_table_name          = "${local.vhs_table_name}"
    reindex_complete_topic_arn = "${module.reindex_jobs_complete_topic.arn}"
    reindex_jobs_queue_id      = "${module.reindexer_queue.id}"
    metrics_namespace          = "reindexer"
  }

  env_vars_length = 4

  cluster_name               = "${local.catalogue_pipeline_cluster_name}"
  vpc_id                     = "${local.vpc_services_id}"
  alb_cloudwatch_id          = "${local.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${local.alb_listener_https_arn}"
  alb_listener_http_arn      = "${local.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false
}

resource "aws_iam_role_policy" "ecs_reindexer_task_sns" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${module.reindex_jobs_complete_topic.publish_policy}"
}

resource "aws_iam_role_policy" "reindexer_reindexer_task_cloudwatch_metric" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "reindexer_allow_table_access" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${local.vhs_dynamodb_full_access_policy}"
}
