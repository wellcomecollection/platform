module "lambda_update_api_size" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  name        = "update_api_size"
  description = "When the prod API changes between Romulus/Remus, update the desired service sizes."

  environment_variables = {
    INFRA_BUCKET = "${var.infra_bucket}"
    TARGET_TOPIC = "${local.service_scheduler_topic_name}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_key          = "lambdas/catalogue_api/update_api_size.zip"
}

resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${module.lambda_update_api_size.function_name}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_update_api_size.arn}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${data.aws_s3_bucket.infra.arn}/prod_api"
}
