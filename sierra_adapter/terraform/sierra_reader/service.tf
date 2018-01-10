locals {
  service_name = "sierra_${var.resource_type}_reader"
}

data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_reader_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/service?ref=v5.3.0"
  name   = "${local.service_name}"

  app_uri = "${var.ecr_repository_url}:${var.release_id}"

  env_vars = {
    resource_type = "${var.resource_type}"

    windows_queue_url = "${module.windows_queue.id}"
    bucket_name = "${var.bucket_name}"

    metrics_namespace = "${local.service_name}"

    sierra_api_url      = "${var.sierra_api_url}"
    sierra_oauth_key    = "${var.sierra_oauth_key}"
    sierra_oauth_secret = "${var.sierra_oauth_secret}"
    sierra_fields       = "${var.sierra_fields}"

    batch_size = 50
  }

  env_vars_length = 9

  path_pattern = "/${local.service_name}/*"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200

  cluster_id                   = "${data.aws_ecs_cluster.cluster.arn}"
  vpc_id                       = "${var.vpc_id}"
  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  listener_https_arn           = "${var.alb_listener_https_arn}"
  listener_http_arn            = "${var.alb_listener_http_arn}"
  client_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  server_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"

  https_domain = "services.wellcomecollection.org"
}