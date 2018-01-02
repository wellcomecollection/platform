module "sierra_merger_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v5.2.2"
  name   = "sierra_${var.resource_type}_merger"

  source_queue_name = "${var.dynamo_updates_queue_name}"
  source_queue_arn  = "${var.dynamo_updates_queue_arn}"

  ecr_repository_url = "${var.ecr_repository_url}"
  release_id         = "${var.release_id}"

  env_vars = {
    windows_queue_url = "${var.dynamo_updates_queue_url}"
    metrics_namespace = "sierra_${var.resource_type}_merger"
    dynamo_table_name = "${var.target_dynamo_table_name}"
  }

  env_vars_length = 3

  alb_priority = "${var.alb_priority}"

  cluster_name               = "${var.cluster_name}"
  vpc_id                     = "${var.vpc_id}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
