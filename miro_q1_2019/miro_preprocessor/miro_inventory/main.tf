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

# This is awful, but we cannot count on modules
# I don't think the required refactor to make this sensible is worth the time
module "miro_inventory_lambda_trigger_topic_1" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.lambda_trigger_topic_arns[0]}"
}

module "miro_inventory_lambda_trigger_topic_2" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_inventory_lambda.function_name}"
  lambda_function_arn  = "${module.miro_inventory_lambda.arn}"

  sns_trigger_arn = "${var.lambda_trigger_topic_arns[1]}"
}
