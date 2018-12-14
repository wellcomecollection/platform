data "aws_s3_bucket_object" "package" {
  bucket = "${var.infra_bucket}"
  key    = "${var.lambda_s3_key}"
}

resource "aws_lambda_function" "lambda_trigger_bag_ingest_monitoring" {
  description   = "Trigger the ingest of bags to test storage services"
  function_name = "${var.name}"

  s3_bucket         = "${data.aws_s3_bucket_object.package.bucket}"
  s3_key            = "${data.aws_s3_bucket_object.package.key}"
  s3_object_version = "${data.aws_s3_bucket_object.package.version_id}"

  role    = "${module.lambda_trigger_bag_ingest_iam.role_arn}"
  handler = "trigger_bag_ingest.main"
  runtime = "python3.6"
  timeout = 10

  memory_size = "128"

  dead_letter_config = {
    target_arn = "${module.lambda_trigger_bag_ingest_monitoring.dlq_arn}"
  }

  environment = {
    variables = {
      BAG_PATHS         = "${var.bag_paths}"
      INGESTS_BUCKET    = "${var.ingest_bucket_name}"
      STORAGE_SPACE     = "${var.storage_space}"
      API_URL           = "${var.api_url}"
      OAUTH_DETAILS_ENC = "${var.oauth_details_enc}"
    }
  }
}
