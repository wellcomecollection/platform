module "bibs_pipeline" {
  source = "pipeline"

  resource_type = "bibs"
}

module "items_pipeline" {
  source = "pipeline"

  resource_type = "items"
}

module "sierra_window_generator_bibs" {
  source                 = "sierra_window_generator"
  window_length_minutes  = "30"
  lambda_trigger_minutes = "15"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  resource_type          = "bibs"
}

module "sierra_window_generator_items" {
  source                 = "sierra_window_generator"
  window_length_minutes  = "30"
  lambda_trigger_minutes = "15"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  resource_type          = "items"
}

module "sierra_to_dynamo_bibs" {
  source             = "sierra_to_dynamo"
  resource_type      = "bibs"
  windows_topic_name = "${module.sierra_window_generator_bibs.topic_name}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${module.sierra_adapter_cluster.cluster_name}"

  ecr_repository_url = "${module.bibs_pipeline.to_dynamo_repository_url}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  release_id = "${var.release_ids["sierra_bibs_to_dynamo"]}"

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_bibs_fields}"

  vpc_id = "${module.vpc_sierra_adapter.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "sierra_to_dynamo_items" {
  source             = "sierra_to_dynamo"
  resource_type      = "items"
  windows_topic_name = "${module.sierra_window_generator_items.topic_name}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${module.sierra_adapter_cluster.cluster_name}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  ecr_repository_url = "${module.items_pipeline.to_dynamo_repository_url}"

  release_id = "${var.release_ids["sierra_items_to_dynamo"]}"

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_items_fields}"

  vpc_id = "${module.vpc_sierra_adapter.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "sierra_bib_merger" {
  source                   = "sierra_merger"
  resource_type            = "bibs"
  dynamo_events_topic_name = "${module.sierra_bib_merger_events_topic.name}"

  target_dynamo_table_name = "${aws_dynamodb_table.sierradata_table.name}"
  target_dynamo_table_arn  = "${aws_dynamodb_table.sierradata_table.arn}"

  ecr_repository_url = "${module.bibs_pipeline.merger_repository_url}"
  release_id         = "${var.release_ids["sierra_bib_merger"]}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${module.sierra_adapter_cluster.cluster_name}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  vpc_id = "${module.vpc_sierra_adapter.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}

module "sierra_item_merger" {
  source                   = "sierra_merger"
  resource_type            = "items"
  dynamo_events_topic_name = "${module.sierra_items_merger_events_topic.name}"

  target_dynamo_table_name = "${aws_dynamodb_table.sierradata_table.name}"
  target_dynamo_table_arn  = "${aws_dynamodb_table.sierradata_table.arn}"

  ecr_repository_url = "${module.items_pipeline.merger_repository_url}"
  release_id         = "${var.release_ids["sierra_item_merger"]}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${module.sierra_adapter_cluster.cluster_name}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  vpc_id = "${module.vpc_sierra_adapter.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}
