module "image_sorter_lambda" {
  source = "../../terraform/lambda"
  source_dir = "${path.module}/target"
  description = "Sort blobs of Miro image metadata into different SNS topics"
  name = "miro_image_sorter"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    TOPIC_COLD_STORE = "${var.topic_cold_store_arn}"
    TOPIC_TANDEM_VAULT = "${var.topic_tandem_vault_arn}"
    TOPIC_DIGITAL_LIBRARY = "${var.topic_digital_library_arn}"
  }
}
