module "sierra_window_generator_bibs" {
  source                 = "sierra_window_generator"
  window_length_minutes  = "30"
  lambda_trigger_minutes = "15"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  resource_type          = "bibs"
}

module "sierra_to_dynamo_bibs" {
  source             = "sierra_to_dynamo"
  resource_type      = "bibs"
  windows_topic_name = "${module.sierra_window_generator_bibs.topic_name}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${data.terraform_remote_state.catalogue_pipeline.ecs_services_cluster_name}"

  alb_priority               = 106
  alb_server_error_alarm_arn = "${local.services_alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.services_alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${local.services_alb_cloudwatch_id}"
  alb_listener_http_arn      = "${local.services_alb_listener_http_arn}"
  alb_listener_https_arn     = "${local.services_alb_listener_https_arn}"

  ecr_repository_url = "${module.ecr_repository_sierra_to_dynamo.repository_url}"
  release_id         = "${var.release_ids["sierra_to_dynamo"]}"

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_bibs_fields}"

  vpc_id = "${local.services_vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "sierra_bib_merger" {
  source                   = "sierra_merger"
  resource_type            = "bibs"
  dynamo_events_topic_name = "${module.sierra_bib_merger_events_topic.name}"

  target_dynamo_table_name = "${aws_dynamodb_table.sierradata_table.name}"
  target_dynamo_table_arn  = "${aws_dynamodb_table.sierradata_table.arn}"

  ecr_repository_url = "${module.ecr_repository_sierra_bib_merger.repository_url}"
  release_id         = "${var.release_ids["sierra_bib_merger"]}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  cluster_name  = "${data.terraform_remote_state.catalogue_pipeline.ecs_services_cluster_name}"

  alb_priority               = 107
  alb_server_error_alarm_arn = "${local.services_alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.services_alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${local.services_alb_cloudwatch_id}"
  alb_listener_http_arn      = "${local.services_alb_listener_http_arn}"
  alb_listener_https_arn     = "${local.services_alb_listener_https_arn}"

  vpc_id = "${local.services_vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}
