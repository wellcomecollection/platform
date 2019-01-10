module "lambda_trigger_bag_ingest" {
  source = "lambda_trigger_bag_ingest"

  name                   = "archive_trigger_bag_ingest"
  lambda_s3_key          = "lambdas/archive/lambdas/trigger_bag_ingest.zip"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${local.infra_bucket}"
  oauth_details_enc      = "${var.archive_oauth_details_enc}"
  bag_paths              = "b22454408.zip"
  ingest_bucket_name     = "${local.ingest_bucket_name}"
}
