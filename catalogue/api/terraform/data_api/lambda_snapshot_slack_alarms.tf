module "lambda_snapshot_slack_alarms" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/data_api/snapshot_slack_alarms.zip"

  name        = "snapshot_slack_alarms"
  description = "Post a notification to Slack when a snapshot fails"
  timeout     = 10

  environment_variables = {
    CRITICAL_SLACK_WEBHOOK = "${var.critical_slack_webhook}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

data "aws_iam_policy_document" "read_from_snapshots_bucket" {
  statement {
    actions = [
      "s3:Head*",
      "s3:List*",
    ]

    resources = [
      "${aws_s3_bucket.public_data.arn}/",
      "${aws_s3_bucket.public_data.arn}/*",
    ]
  }
}

resource "aws_iam_role_policy" "snapshot_alarms_read_from_bucket" {
  role   = "${module.lambda_snapshot_slack_alarms.role_name}"
  policy = "${data.aws_iam_policy_document.read_from_snapshots_bucket.json}"
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_snapshot_slack_alarms.function_name}"
  lambda_function_arn  = "${module.lambda_snapshot_slack_alarms.arn}"
  sns_trigger_arn      = "${module.snapshot_alarm_topic.arn}"
}
