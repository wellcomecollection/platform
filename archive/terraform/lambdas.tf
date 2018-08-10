module "archive_asset_loookup" {
  source = "api_gw_lambda"

  name        = "archive_asset_loookup"
  description = "Looks up archived assets."

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

module "archive_bag_request" {
  source = "api_gw_lambda"

  name        = "archive_bag_request"
  description = "Requests bag ingests."

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}
