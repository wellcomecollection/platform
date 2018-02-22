module "s3_demultiplexer_lambda" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.3"
  s3_bucket   = "${var.infra_bucket}"
  s3_key      = "lambdas/sierra_adapter/s3_demultiplexer.zip"
  module_name = "s3_demultiplexer"

  description     = "Split JSON bundles of ${var.resource_type} and send them to SNS"
  name            = "s3_${var.resource_type}_demultiplexer"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    "TOPIC_ARN" = "${module.demultiplexer_topic.arn}"
  }

  timeout = 90
}

resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${module.s3_demultiplexer_lambda.function_name}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.s3_demultiplexer_lambda.function_name}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${data.aws_s3_bucket.sierra_data.arn}"
}

resource "aws_iam_role_policy" "allow_s3_read" {
  role   = "${module.s3_demultiplexer_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_read.json}"
}

data "aws_iam_policy_document" "allow_s3_read" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${data.aws_s3_bucket.sierra_data.arn}",
      "${data.aws_s3_bucket.sierra_data.arn}/*",
    ]
  }
}

resource "aws_iam_role_policy" "allow_sns_publish" {
  role   = "${module.s3_demultiplexer_lambda.role_name}"
  policy = "${module.demultiplexer_topic.publish_policy}"
}
