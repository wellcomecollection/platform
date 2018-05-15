module "matcher" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "matcher"

  source_queue_name  = "${module.matcher_queue.name}"
  source_queue_arn   = "${module.matcher_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_matcher.repository_url}"
  release_id         = "${var.release_ids["matcher"]}"

  env_vars = {
    queue_url             = "${module.matcher_queue.id}"
    bucket_name       = "${module.vhs_recorder.bucket_name}"
    metrics_namespace              = "matcher"
    topic_arn           = "${module.redirects_topic.arn}"
  }

  env_vars_length = 4

  memory = 2048
  cpu    = 512

  cluster_name = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id       = "${module.vpc_services.vpc_id}"

  alb_priority = 108

  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15
}


module "matcher_dynamo_to_sns" {
  source = "../../shared_infra/dynamo_to_sns"

  name           = "sourcedata"
  src_stream_arn = "${module.vhs_recorder.table_stream_arn}"
  dst_topic_arn  = "${module.recorded_works_topic.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  infra_bucket = "${var.infra_bucket}"
}