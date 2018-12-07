module "lambda_miro_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_miro_transformer"
  description = "Transform miro source data and send to ES."

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "miro"
    ES_DOC_TYPE = "miro_record"
  }

  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${var.aws_region}"

  vhs_read_policy       = "${local.miro_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  trigger_topic_arns = [
    "${local.miro_reindex_topic_arn}",
  ]
}

module "lambda_miro_inventory_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_miro_inventory_transformer"
  description = "Transform miro inventory source data and send to ES."

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "miro_inventory"
    ES_DOC_TYPE = "miro_inventory_record"
  }

  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${var.aws_region}"

  vhs_read_policy       = "${local.miro_inventory_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  trigger_topic_arns = [
    "${local.miro_inventory_topic_arn}"
  ]

}

module "lambda_sierra_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_sierra_transformer"
  description = "Transform sierra source data and send to ES."

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "sierra"
    ES_DOC_TYPE = "sierra_record"
  }

  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${var.aws_region}"

  vhs_read_policy       = "${local.sierra_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  trigger_topic_arns = [
    "${local.sierra_topic_arn}"
  ]
}
