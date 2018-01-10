module "bibs_window_generator" {
  source = "sierra_window_generator"

  resource_type = "bibs"

  window_length_minutes    = 30
  trigger_interval_minutes = 15

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
