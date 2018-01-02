module "sierra_bibs_pipeline" {
  source = "pipeline"

  resource_type = "bibs"

  window_length_minutes   = 30
  window_interval_minutes = 15

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_bibs_fields}"

  sierradata_table_name = "${aws_dynamodb_table.sierradata_table.name}"

  release_ids = "${var.release_ids}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  cluster_name = "${module.sierra_adapter_cluster.cluster_name}"
  vpc_id       = "${module.vpc_sierra_adapter.vpc_id}"

  account_id   = "${data.aws_caller_identity.current.account_id}"
}

module "sierra_items_pipeline" {
  source = "pipeline"

  resource_type = "items"

  window_length_minutes   = 30
  window_interval_minutes = 15

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_items_fields}"

  sierradata_table_name = "${aws_dynamodb_table.sierradata_table.name}"

  release_ids = "${var.release_ids}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  cluster_name = "${module.sierra_adapter_cluster.cluster_name}"
  vpc_id       = "${module.vpc_sierra_adapter.vpc_id}"

  account_id   = "${data.aws_caller_identity.current.account_id}"
}
