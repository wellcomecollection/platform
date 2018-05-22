resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${aws_s3_bucket.sierra_adapter.id}"

  lambda_function {
    lambda_function_arn = "${module.bibs_reader.demultiplexer_arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "records_bibs/"
    filter_suffix       = ".json"
  }

  lambda_function {
    lambda_function_arn = "${module.items_reader.demultiplexer_arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "records_items/"
    filter_suffix       = ".json"
  }
}
