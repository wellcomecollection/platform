module "reindexer" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v6.4.0"
  name   = "reindexer"

  source_queue_name = "${module.reindexer_queue.name}"
  source_queue_arn  = "${module.reindexer_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_ingestor.repository_url}"
  release_id         = "${var.release_ids["reindexer"]}"

  cpu    = 512
  memory = 1024

  env_vars = {
    dynamo_table_name          = "${module.versioned-hybrid-store.table_name}"
    reindex_complete_topic_arn = "${data.aws_sns_topic.reindex_jobs_complete_topic.arn}"
    reindex_jobs_queue_url     = "${module.reindexer_queue.arn}"
    metrics_namespace          = "reindexer"
  }

  env_vars_length = 4

  cluster_name               = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}

data "aws_sns_topic" "reindex_jobs_complete_topic" {
  name = "${module.reindex_jobs_complete_topic.name}"
}

# Role policies for the reindexer

resource "aws_iam_role_policy" "ecs_reindexer_task_sns" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${module.reindex_jobs_complete_topic.publish_policy}"
}

resource "aws_iam_role_policy" "reindexer_tracker_table" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_tracker_table.json}"
}

resource "aws_iam_role_policy" "reindexer_reindexer_task_cloudwatch_metric" {
  role   = "${module.reindexer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
