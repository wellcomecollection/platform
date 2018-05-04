module "recorder" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "recorder"

  source_queue_name  = "${module.recorder_queue.name}"
  source_queue_arn   = "${module.recorder_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_recorder.repository_url}"
  release_id         = "${var.release_ids["recorder"]}"

  env_vars = {
    queue_url           = "${module.recorder_queue.id}"
    message_bucket_name = "${aws_s3_bucket.messages.id}"
    sqs_max_messages    = 10
  }

  memory = 2048
  cpu    = 512

  env_vars_length = 3

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
}
