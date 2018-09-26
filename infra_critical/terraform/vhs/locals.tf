locals {
  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  table_name        = "${var.table_name_prefix}${var.name}"
  local_bucket_name = "${var.bucket_name_prefix}${lower(var.name)}"

  bucket_name = "${var.bucket_name == "" ? local.local_bucket_name : var.bucket_name}"

  table_arn  = "arn:aws:dynamodb:${var.aws_region}:${var.account_id}:table/${local.table_name}"
  bucket_arn = "arn:aws:s3:::${local.bucket_name}"
}
