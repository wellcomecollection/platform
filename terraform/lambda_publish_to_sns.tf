/** Lambda for publishing ECS service schedules to an SNS topic. */

module "publish_to_sns_lambda" {
  source      = "./lambda"
  name        = "publish_to_sns"
  description = "Publish an ECS service schedule to SNS"
  filename    = "../lambdas/publish_to_sns.py"
}

module "schedule_calm_adapter" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.publish_to_sns_lambda.function_name}"
  lambda_function_arn     = "${module.publish_to_sns_lambda.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.weekdays_at_7am.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.weekdays_at_7am.name}"

  input = <<EOF
{
  "topic_arn": "${aws_sns_topic.service_scheduler_topic.arn}",
  "cluster": "${aws_ecs_cluster.services.name}",
  "service": "${module.calm_adapter.service_name}",
  "desired_count": 1
}
EOF
}
