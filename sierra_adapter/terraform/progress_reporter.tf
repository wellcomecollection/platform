module "progress_reporter" {
  source = "sierra_progress_reporter"

  window_length_minutes  = 1
  s3_adapter_bucket_name = "${aws_s3_bucket.sierra_adapter.id}"
  slack_access_token     = "${var.non_critical_slack_webhook}"

  infra_bucket = "${var.infra_bucket}"
}
