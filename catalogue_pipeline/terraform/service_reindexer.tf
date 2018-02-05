module "miro_reindexer" {
  source = "git::https://github.com/wellcometrust/terraform.git//service?ref=v5.0.2"
  name   = "miro_reindexer"

  cluster_id = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id     = "${module.vpc_services.vpc_id}"
  app_uri    = "${module.ecr_repository_reindexer.repository_url}:${var.release_ids["reindexer"]}"

  listener_https_arn = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  listener_http_arn  = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  path_pattern       = "/miro_reindexer/*"
  alb_priority       = "104"

  cpu    = 512
  memory = 1024

  desired_count = "0"

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  env_vars = {
    miro_table_name    = "${aws_dynamodb_table.miro_table.name}"
    reindex_table_name = "${aws_dynamodb_table.reindex_tracker.name}"
    metrics_namespace  = "miro-reindexer"
  }

  env_vars_length = 3

  loadbalancer_cloudwatch_id   = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"

  https_domain = "services.wellcomecollection.org"
}
