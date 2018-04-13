locals {
  loris_cloudfront_id = "${data.terraform_remote_state.loris.loris_cloudfront_id}"

  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
}
