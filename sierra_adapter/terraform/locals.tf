locals {
  services_alb_listener_http_arn  = "${data.terraform_remote_state.catalogue_pipeline.services_alb_listener_http_arn}"
  services_alb_listener_https_arn = "${data.terraform_remote_state.catalogue_pipeline.services_alb_listener_https_arn}"

  services_alb_cloudwatch_id = "${data.terraform_remote_state.catalogue_pipeline.services_alb_cloudwatch_id}"

  services_alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  services_alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  services_vpc_id = "${data.terraform_remote_state.catalogue_pipeline.vpc_services_id}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
}
