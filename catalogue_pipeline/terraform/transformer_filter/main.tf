module "prefilter_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${var.name}_transformer_prefilter"
}

module "dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns"

  name           = "${var.name}"
  src_stream_arn = "${var.src_stream_arn}"
  dst_topic_arn  = "${module.prefilter_topic.arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}

module "transformer_filter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda?ref=v1.2.0"

  name        = "${var.name}_transformer_filter"
  module_name = "transformer_sns_filter"
  description = "Filters DynamoDB events for the ${var.name} transformer"

  environment_variables = {
    TOPIC_ARN = "${var.dst_topic_arn}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/catalogue_pipeline/transformer_sns_filter.zip"
}

module "trigger_transformer_filter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_sns?ref=v1.0.0"

  sns_trigger_arn      = "${module.prefilter_topic.arn}"
  lambda_function_arn  = "${module.transformer_filter.arn}"
  lambda_function_name = "${module.transformer_filter.role_name}"
}

data "aws_iam_policy_document" "publish_to_topic" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${var.dst_topic_arn}",
    ]
  }
}

resource "aws_iam_role_policy" "transformer_filter_publish_permissions" {
  role   = "${module.transformer_filter.role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_topic.json}"
}
