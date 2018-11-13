module "lambda_miro_transformer" {
  source = "./reporting_lambda"

  name        = "miro_transformer"
  description = "Transform miro source data and send to ES."

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "miro"
    ES_DOC_TYPE = "miro_record"
  }

  trigger_topic_arn     = "${local.miro_topic_arn}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "lambda_miro_inventory_transformer" {
  source = "./reporting_lambda"

  name        = "miro_inventory_transformer"
  description = "Transform miro inventory source data and send to ES."

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "miro_inventory"
    ES_DOC_TYPE = "miro_inventory_record"
  }

  trigger_topic_arn     = "${local.miro_inventory_topic_arn}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}
