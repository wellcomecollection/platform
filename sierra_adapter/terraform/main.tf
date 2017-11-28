module "sierra_window_generator_items" {
  source                 = "sierra_window_generator"
  window_length_minutes  = "10"
  lambda_trigger_minutes = "5"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  resource_type          = "items"
}

module "sierra_window_generator_bibs" {
  source                 = "sierra_window_generator"
  window_length_minutes  = "30"
  lambda_trigger_minutes = "15"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  resource_type          = "bibs"
}
