resource "aws_s3_bucket_notification" "private_data_bucket_notification" {
  bucket = "${var.bucket_name}"

  lambda_function {
    lambda_function_arn = "${module.snapshot_convertor_job_generator_lambda.arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "elasticdump/"
    filter_suffix       = ".json"
  }
}
