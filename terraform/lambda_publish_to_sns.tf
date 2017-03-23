/*
  Lambda for publishing ECS service schedules to an SNS topic.
 */

module "publish_to_sns_lambda" {
  source      = "./lambda"
  name        = "publish_to_sns"
  description = "Publish an ECS service schedule to SNS"
  filename    = "../lambdas/publish_to_sns.py"
}

module "publish_to_sns_trigger" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.publish_to_sns_lambda.function_name}"
  lambda_function_arn     = "${module.publish_to_sns_lambda.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.once_a_day.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.once_a_day.name}"

  input = <<EOF
{
  "topic_arn": "${aws_sns_topic.service_scheduler_topic.arn}",
  "cluster": "${aws_ecs_cluster.services.name}",
  "service": "calm_adapter",
  "desired_count": 1
}
EOF
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
