module "merger" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.3.0"
  name   = "merger"

  source_queue_name  = "${module.merger_queue.name}"
  source_queue_arn   = "${module.merger_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_merger.repository_url}"
  release_id         = "${var.release_ids["merger"]}"

  env_vars = {
    merger_queue_id          = "${module.merger_queue.id}"
    merger_topic_arn         = "${module.merged_works_topic.arn}"
    vhs_recorder_bucket_name = "${module.vhs_recorder.bucket_name}"
    vhs_recorder_table_name  = "${module.vhs_recorder.table_name}"
    metrics_namespace        = "merger"
    log_level                = "INFO"
  }

  memory = 2048
  cpu    = 512

  cluster_name = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id       = "${module.vpc_services.vpc_id}"

  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15

  log_retention_in_days = 30
}
