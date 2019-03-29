module "lambda_miro_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_miro_transformer"
  description = "Transform miro source data and send to ES."

  environment_variables = {
    ES_URL      = "${local.es_url}"
    ES_USER     = "${local.es_username}"
    ES_PASS     = "${local.es_password}"
    ES_INDEX    = "miro"
    ES_DOC_TYPE = "miro_record"
  }

  vhs_read_policy       = "${local.miro_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  topic_arns = [
    "${local.miro_reindex_topic_arn}",
    "${local.miro_updates_topic_arn}",
  ]

  topic_count = 2
}

module "lambda_miro_inventory_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_miro_inventory_transformer"
  description = "Transform miro inventory source data and send to ES."

  environment_variables = {
    ES_URL      = "${local.es_url}"
    ES_USER     = "${local.es_username}"
    ES_PASS     = "${local.es_password}"
    ES_INDEX    = "miro_inventory"
    ES_DOC_TYPE = "miro_inventory_record"
  }

  vhs_read_policy       = "${local.miro_inventory_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  topic_arns = [
    "${local.miro_inventory_topic_arn}",
  ]
}

module "lambda_sierra_transformer" {
  source = "./reporting_lambda"

  name        = "reporting_sierra_transformer"
  description = "Transform sierra source data and send to ES."

  environment_variables = {
    ES_URL      = "${local.es_url}"
    ES_USER     = "${local.es_username}"
    ES_PASS     = "${local.es_password}"
    ES_INDEX    = "sierra"
    ES_DOC_TYPE = "sierra_record"
  }

  vhs_read_policy       = "${local.sierra_vhs_read_policy}"
  error_alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  topic_arns = [
    "${local.sierra_reindex_topic_arn}",
    "${local.sierra_updates_topic_arn}",
  ]

  topic_count = 2
}
