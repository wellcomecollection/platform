locals {
  sierra_to_dynamo_name = "sierra_${var.resource_type}_to_dynamo"
}

data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_to_dynamo_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/service?ref=v5.3.0"
  name   = "${local.sierra_to_dynamo_name}"

  app_uri = "${module.ecr_repository_sierra_to_dynamo.repository_url}:${var.sierra_to_dynamo_release_id}"

  env_vars = {
    windows_queue_url = "${module.windows_queue.id}"
    metrics_namespace = "sierra_${var.resource_type}_to_dynamo"

    dynamo_table_name = "${aws_dynamodb_table.sierra_table.id}"

    sierra_api_url      = "${var.sierra_api_url}"
    sierra_oauth_key    = "${var.sierra_oauth_key}"
    sierra_oauth_secret = "${var.sierra_oauth_secret}"
    sierra_fields       = "${var.sierra_fields}"
  }

  path_pattern = "/${local.sierra_to_dynamo_name}/*"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200

  env_vars_length = 7

  cluster_id                   = "${data.aws_ecs_cluster.cluster.arn}"
  vpc_id                       = "${var.vpc_id}"
  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  listener_https_arn           = "${var.alb_listener_https_arn}"
  listener_http_arn            = "${var.alb_listener_http_arn}"
  client_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  server_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"

  https_domain = "services.wellcomecollection.org"
}

module "sierra_merger_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v5.3.0"
  name   = "sierra_${local.resource_type_singular}_merger"

  source_queue_name = "${module.updates_queue.name}"
  source_queue_arn  = "${module.updates_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_sierra_merger.repository_url}"
  release_id         = "${var.sierra_merger_release_id}"

  env_vars = {
    windows_queue_url = "${module.updates_queue.id}"
    metrics_namespace = "sierra_${local.resource_type_singular}_merger"
    dynamo_table_name = "${var.merged_dynamo_table_name}"
  }

  env_vars_length = 3

  cluster_name               = "${var.cluster_name}"
  vpc_id                     = "${var.vpc_id}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
