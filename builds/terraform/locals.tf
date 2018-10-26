locals {
  infra_bucket_arn = "${data.terraform_remote_state.shared_infra.infra_bucket_arn}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
}
