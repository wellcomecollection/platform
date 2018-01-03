module "sierra_to_dynamo_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v5.2.2"
  name   = "sierra_${var.resource_type}_to_dynamo"

  source_queue_name  = "${var.windows_queue_name}"
  source_queue_arn   = "${var.windows_queue_arn}"
  ecr_repository_url = "${module.ecr_repository.repository_url}"
  release_id         = "${var.release_id}"

  env_vars = {
    windows_queue_url = "${var.windows_queue_url}"
    metrics_namespace = "sierra_to_dynamo-${var.resource_type}"

    dynamo_table_name = "${aws_dynamodb_table.sierra_table.id}"

    sierra_api_url       = "${var.sierra_api_url}"
    sierra_oauth_key     = "${var.sierra_oauth_key}"
    sierra_oauth_secret  = "${var.sierra_oauth_secret}"
    sierra_resource_type = "${var.resource_type}"
    sierra_fields        = "${var.sierra_fields}"
  }

  env_vars_length = 8

  alb_priority = "${var.alb_priority}"

  cluster_name               = "${var.cluster_name}"
  vpc_id                     = "${var.vpc_id}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
