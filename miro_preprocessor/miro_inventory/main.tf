module "miro_inventory_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/miro_preprocessor/miro_inventory.zip"

  description     = "Push miro inventory data to ES"
  name            = "miro_inventory"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  timeout = "120"

  environment_variables = {
    ES_CLUSTER_URL = "${var.cluster_url}"
    ES_USERNAME    = "${var.es_username}"
    ES_PASSWORD    = "${var.es_passsword}"

    ES_INDEX = "miro"
    ES_TYPE  = "image"
    ID_FIELD = "image_data.image_no_calc"
  }
}

module "miro_inventory_lambda_trigger_cold_store_topic" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.cold_store_topic_arn}"
}

module "miro_inventory_lambda_trigger_tandem_vault_topic" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.tandem_vault_topic_arn}"
}

module "miro_inventory_lambda_trigger_catalogue_api_topic" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.catalogue_api_topic_arn}"
}

module "miro_inventory_lambda_trigger_digital_library_topic" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.digital_library_topic_arn}"
}

module "miro_inventory_lambda_trigger_none_topic" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.none_topic_arn}"
}
