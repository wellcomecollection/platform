module "sierra_window_generator" {
  source = "../sierra_window_generator"

  resource_type          = "${var.resource_type}"
  window_length_minutes  = "${var.window_length_minutes}"
  lambda_trigger_minutes = "${var.window_interval_minutes}"

  dlq_alarm_arn          = "${var.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  account_id             = "${var.account_id}"
}

module "sierra_to_dynamo" {
  source = "../sierra_to_dynamo"

  release_id = "${var.release_ids["sierra_${var.resource_type}_to_dynamo"]}"

  resource_type      = "${var.resource_type}"

  windows_queue_name = "${module.sierra_window_generator.queue_name}"
  windows_queue_arn  = "${module.sierra_window_generator.queue_arn}"
  windows_queue_url  = "${module.sierra_window_generator.queue_url}"

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_fields}"

  alb_priority               = "${random_integer.priority_sierra_to_dynamo.result}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  dlq_alarm_arn          = "${var.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"

  account_id = "${var.account_id}"
}

module "sierra_merger" {
  source = "../sierra_merger"

  release_id = "${var.release_ids["sierra_${replace("${var.resource_type}", "s", "")}_merger"]}"

  resource_type = "${var.resource_type}"

  dynamo_updates_queue_name = "${module.sierra_to_dynamo.queue_name}"
  dynamo_updates_queue_arn  = "${module.sierra_to_dynamo.queue_arn}"
  dynamo_updates_queue_url  = "${module.sierra_to_dynamo.queue_url}"

  target_dynamo_table_name = "${var.sierradata_table_name}"

  alb_priority               = "${random_integer.priority_sierra_merger.result}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  account_id = "${var.account_id}"
}
