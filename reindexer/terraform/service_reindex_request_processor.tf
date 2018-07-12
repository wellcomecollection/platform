module "reindex_request_processor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.2.2"
  name   = "reindex_request_processor"

  source_queue_name = "${module.reindex_requests_queue.name}"
  source_queue_arn  = "${module.reindex_requests_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_reindex_request_processor.repository_url}"
  release_id         = "${var.release_ids["reindex_request_processor"]}"

  cpu    = 512
  memory = 2048

  env_vars = {
    dynamo_table_name          = "${local.vhs_table_name}"
    reindex_requests_queue_id  = "${module.reindex_requests_queue.id}"
    metrics_namespace          = "reindex_request_processor"
  }
  env_vars_length = 3

  cluster_name               = "${local.catalogue_pipeline_cluster_name}"
  vpc_id                     = "${local.vpc_services_id}"
  alb_cloudwatch_id          = "${local.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${local.alb_listener_https_arn}"
  alb_listener_http_arn      = "${local.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  log_retention_in_days = 30
}

resource "aws_iam_role_policy" "reindex_processor_task_cloudwatch_metric" {
  role   = "${module.reindex_request_processor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "reindex_processor_allow_table_access" {
  role   = "${module.reindex_request_processor.task_role_name}"
  policy = "${local.vhs_full_access_policy}"
}
