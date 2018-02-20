locals {
  vhs_dynamodb_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline.vhs_dynamo_full_access_policy}"
  vhs_dynamodb_update_policy      = "${data.terraform_remote_state.catalogue_pipeline.vhs_dynamodb_update_policy}"
  vhs_table_name                  = "${data.terraform_remote_state.catalogue_pipeline.vhs_table_name}"
  vhs_table_stream_arn            = "${data.terraform_remote_state.catalogue_pipeline.vhs_table_stream_arn}"

  vpc_services_id = "${data.terraform_remote_state.catalogue_pipeline.vpc_services_id}"

  alb_cloudwatch_id          = "${data.terraform_remote_state.catalogue_pipeline.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${data.terraform_remote_state.catalogue_pipeline.alb_listener_https_arn}"
  alb_listener_http_arn      = "${data.terraform_remote_state.catalogue_pipeline.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  catalogue_pipeline_cluster_name = "${data.terraform_remote_state.catalogue_pipeline.cluster_name}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}
