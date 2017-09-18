module "image_sorter_lambda" {
  source          = "../../terraform/lambda"
  source_dir      = "${path.module}/target"
  description     = "Sort blobs of Miro image metadata into different SNS topics"
  name            = "miro_image_sorter"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    S3_MIRODATA_ID      = "${var.s3_miro_data_id}"
    TOPIC_COLD_STORE    = "${var.topic_cold_store_publish_arn}"
    TOPIC_TANDEM_VAULT  = "${var.topic_tandem_vault_publish_arn}"
    TOPIC_CATALOGUE_API = "${var.topic_catalogue_api_publish_arn}"
  }
}
