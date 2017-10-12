module "miro_inventory_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/miro_preprocessor/miro_inventory.zip"

  description     = "Push miro inventory data to ES"
  name            = "miro_inventory"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  timeout = "30"

  environment_variables = {
    ES_CLUSTER_URL = "${var.cluster_url}"
    ES_USERNAME    = "${var.es_username}"
    ES_PASSWORD    = "${var.es_passsword}"

    ES_INDEX = "miro"
    ES_TYPE  = "image"
    ID_FIELD = "image_data.image_no_calc"
  }
}
