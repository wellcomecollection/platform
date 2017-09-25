module "image_sorter_lambda" {
  source          = "../../terraform/lambda"
  source_dir      = "${path.module}/target"
  description     = "Sort blobs of Miro image metadata into different SNS topics"
  name            = "miro_image_sorter"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    S3_MIRODATA_ID        = "${var.s3_miro_data_id}"
    TOPIC_COLD_STORE      = "${var.topic_cold_store_arn}"
    TOPIC_TANDEM_VAULT    = "${var.topic_tandem_vault_arn}"
    TOPIC_CATALOGUE_API   = "${var.topic_catalogue_api_arn}"
    TOPIC_DIGITAL_LIBRARY = "${var.topic_digital_library_arn}"
    TOPIC_NONE            = "${var.topic_none_arn}"
  }

  timeout = "30"
}

resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${module.image_sorter_lambda.function_name}_${var.s3_miro_data_id}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.image_sorter_lambda.function_name}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${var.s3_miro_data_arn}"
}
