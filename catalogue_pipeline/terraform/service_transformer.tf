module "transformer" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v6.4.0"
  name   = "transformer"

  memory = "2560"
  cpu    = "512"

  source_queue_name  = "${module.transformer_queue.name}"
  source_queue_arn   = "${module.transformer_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_transformer.repository_url}"
  release_id         = "${var.release_ids["transformer"]}"

  env_vars = {
    sns_arn              = "${module.id_minter_topic.arn}"
    transformer_queue_id = "${module.transformer_queue.id}"
    metrics_namespace    = "transformer"
    bucket_name          = "${module.versioned-hybrid-store.bucket_name}"
  }

  env_vars_length = 4

  alb_priority = "108"

  cluster_name               = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}

module "transformer_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns"

  name           = "sierra"
  src_stream_arn = "${module.versioned-hybrid-store.table_stream_arn}"
  dst_topic_arn  = "${module.transformer_topic.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
