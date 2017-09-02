resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${var.lambda_function_name}"
  action        = "lambda:InvokeFunction"
  function_name = "${var.lambda_function_arn}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${var.s3_bucket_arn}"
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${var.s3_bucket_id}"

  lambda_function {
    lambda_function_arn = "${var.lambda_function_arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "${var.filter_prefix}"
    filter_suffix       = "${var.filter_suffix}"
  }
}
