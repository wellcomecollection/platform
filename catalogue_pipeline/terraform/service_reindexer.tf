module "miro_reindexer" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "miro_reindexer"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_miro_reindexer_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_reindexer.repository_url}:${var.release_ids["reindexer"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/miro_reindexer/*"
  alb_priority       = "104"
  healthcheck_path   = "/miro_reindexer/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/miro_reindexer.ini"

  cpu    = 512
  memory = 1024

  desired_count = "0"

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    miro_table_name    = "${aws_dynamodb_table.miro_table.name}"
    reindex_table_name = "${aws_dynamodb_table.reindex_tracker.name}"
    metrics_namespace  = "miro-reindexer"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}
