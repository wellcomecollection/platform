locals {
  service_name = "sierra_${var.resource_type}_reader"
}

module "sierra_reader_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "${local.service_name}"

  source_queue_name  = "${module.windows_queue.name}"
  source_queue_arn   = "${module.windows_queue.arn}"
  ecr_repository_url = "${var.ecr_repository_url}"
  release_id         = "${var.release_id}"

  env_vars = {
    resource_type = "${var.resource_type}"

    windows_queue_url = "${module.windows_queue.id}"
    bucket_name       = "${var.bucket_name}"

    metrics_namespace = "${local.service_name}"

    sierra_api_url      = "${var.sierra_api_url}"
    sierra_oauth_key    = "${var.sierra_oauth_key}"
    sierra_oauth_secret = "${var.sierra_oauth_secret}"
    sierra_fields       = "${var.sierra_fields}"

    batch_size = 50
  }

  env_vars_length = 9

  cpu    = 512
  memory = 2048

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15
}
