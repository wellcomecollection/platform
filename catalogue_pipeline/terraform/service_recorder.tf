module "recorder" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.3.0"
  name   = "recorder"

  source_queue_name  = "${module.recorder_queue.name}"
  source_queue_arn   = "${module.recorder_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_recorder.repository_url}"
  release_id         = "${var.release_ids["recorder"]}"

  env_vars = {
    recorder_queue_url             = "${module.recorder_queue.id}"
    message_bucket_name            = "${aws_s3_bucket.messages.id}"
    vhs_recorder_dynamo_table_name = "${module.vhs_recorder.table_name}"
    vhs_recorder_bucket_name       = "${module.vhs_recorder.bucket_name}"
    metrics_namespace              = "recorder"
  }

  memory = 2048
  cpu    = 512

  cluster_name = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id       = "${module.vpc_services.vpc_id}"

  alb_priority = 106

  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15

  log_retention_in_days = 30
}
