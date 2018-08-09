module "archive_asset_loookup" {
  source = "api_gw_lambda"

  name        = "archive_asset_loookup"
  description = "Looks up archived assets."

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}
