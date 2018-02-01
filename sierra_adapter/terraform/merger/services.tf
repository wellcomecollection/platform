locals {
  sierra_to_dynamo_name = "sierra_${var.resource_type}_to_dynamo"
}

data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_merger_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v5.3.0"
  name   = "sierra_${local.resource_type_singular}_merger"

  source_queue_name = "${module.updates_queue.name}"
  source_queue_arn  = "${module.updates_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_sierra_merger.repository_url}"
  release_id         = "${var.release_id}"

  cpu    = 512
  memory = 2048

  env_vars = {
    windows_queue_url = "${module.updates_queue.id}"
    metrics_namespace = "sierra_${local.resource_type_singular}_merger"
    dynamo_table_name = "${var.merged_dynamo_table_name}"
    bucket_name ="${var.bucket_name}"
  }

  env_vars_length = 4

  cluster_name               = "${var.cluster_name}"
  vpc_id                     = "${var.vpc_id}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
