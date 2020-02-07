locals {
  infra_bucket_arn = data.terraform_remote_state.shared_infra.outputs.infra_bucket_arn

  infra_bucket_id = data.terraform_remote_state.shared_infra.outputs.infra_bucket

  lambda_error_alarm_arn = data.terraform_remote_state.shared_infra.outputs.lambda_error_alarm_arn

  platform_read_only_role = "arn:aws:iam::${local.platform_account_id}:role/platform-read_only"
}
