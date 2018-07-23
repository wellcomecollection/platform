module "wiretap" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/catalogue_pipeline/wiretap.zip"

  name        = "wiretap"
  description = "Recording your SNS messages on behalf of shady organisations"
  timeout     = 120

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  environment_variables = {
    "BUCKET" = "wellcomecollection-tmp-wiretap"
  }

  log_retention_in_days = 30
}

resource "aws_s3_bucket" "wiretap" {
  bucket = "wellcomecollection-tmp-wiretap"
}

data "aws_iam_policy_document" "allow_s3_access" {
  statement {
    actions = ["s3:*"]

    resources = [
      "${aws_s3_bucket.wiretap.arn}/*",
      "${aws_s3_bucket.wiretap.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "lambda_gatling_to_cloudwatch_put_metric" {
  role   = "${module.wiretap.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}

module "trigger0" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "arn:aws:sns:eu-west-1:760097843905:reindex_requests"
}

module "trigger1" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.es_ingest_topic_arn}"
}

module "trigger2" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.matched_works_topic_arn}"
}

module "trigger3" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.merged_works_topic_arn}"
}

module "trigger4" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.recorded_works_topic_arn}"
}


module "trigger5" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.transformed_works_topic_arn}"
}


module "trigger6" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.wiretap.function_name}"
  lambda_function_arn  = "${module.wiretap.arn}"
  sns_trigger_arn      = "${module.catalogue_pipeline.transformer_topic_arn}"
}