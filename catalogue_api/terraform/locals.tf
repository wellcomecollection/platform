locals {
  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  namespace = "catalogue-api"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  alb_api_wc_service_lb_security_group_id = "${data.terraform_remote_state.infra_critical.alb_api_wc_service_lb_security_group_id}"
  alb_api_wc_https_listener_arn           = "${data.terraform_remote_state.infra_critical.alb_api_wc_https_listener_arn}"
  alb_api_wc_cloudwatch_id                = "${data.terraform_remote_state.infra_critical.alb_api_wc_cloudwatch_id}"
}
