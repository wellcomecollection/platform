/*
  Lambda for updating ECS service size.

  This is triggered by updates to an SNS topic.
 */

data "archive_file" "update_ecs_service_size" {
  type        = "zip"
  source_file = "../lambdas/update_ecs_service_size.py"
  output_path = "/tmp/update_ecs_service_size.zip"
}

module "update_ecs_service_size_lambda" {
  source      = "./lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  filename    = "${data.archive_file.update_ecs_service_size.output_path}"
}

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name = "update_ecs_service_size"
  role = "${module.update_ecs_service_size_lambda.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_sns_topic_subscription" "topic_lambda" {
  topic_arn = "${aws_sns_topic.service_scheduler_topic.arn}"
  protocol  = "lambda"
  endpoint  = "${module.update_ecs_service_size_lambda.arn}"
}

resource "aws_lambda_permission" "with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = "${module.update_ecs_service_size_lambda.arn}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${aws_sns_topic.service_scheduler_topic.arn}"
}

/*
  Lambda for publishing ECS service schedules to an SNS topic.
 */

data "archive_file" "publish_to_sns_lambda" {
  type        = "zip"
  source_file = "../lambdas/publish_to_sns_lambda.py"
  output_path = "/tmp/publish_to_sns_lambda.zip"
}

module "publish_to_sns_lambda" {
  source      = "./lambda"
  name        = "publish_to_sns"
  description = "Publish an ECS service schedule to SNS"
  filename    = "${data.archive_file.publish_to_sns.output_path}"
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_check_foo" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = "${module.publish_to_sns_lambda.function_name}"
  principal     = "events.amazonaws.com"
  source_arn    = "${aws_cloudwatch_event_rule.once_a_day.arn}"
}

resource "aws_iam_role_policy" "publish_to_sns_lambda_policy" {
  name = "publish_to_sns_policy"
  role = "${module.publish_to_sns_lambda.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "${aws_sns_topic.service_scheduler_topic.arn}"
    }
  ]
}
EOF
}

/*
  Schedules for individual services.
 */

resource "aws_cloudwatch_event_target" "trigger_calm_adapter_daily" {
  rule = "${aws_cloudwatch_event_rule.once_a_day.name}"
  arn  = "${module.publish_to_sns_lambda.arn}"

  input = <<EOF
{
  "topic_arn": "${aws_sns_topic.service_scheduler_topic.arn}",
  "cluster": "${aws_ecs_cluster.services.name}",
  "service": "calm_adapter",
  "desired_count": 1
}
EOF
}
