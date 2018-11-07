module "lambda_miro_transformer" {
  source = "./reporting_lambda"

  name        = "miro_transformer"
  description = "Transform source data and send to ES."

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
