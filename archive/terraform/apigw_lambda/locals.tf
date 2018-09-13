locals {
  infra_bucket           = "${data.terraform_remote_state.shared_infra.infra_bucket}"
  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
}
