locals {
  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  namespace = "catalogue-api"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
}
