# Lambda for tracking task status in dynamo db

module "lambda_task_tracking" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "task_tracking"
  description = "Lambda for tracking task status in dynamo db"
  timeout     = 10

  environment_variables = {
    TABLE_NAME   = "${aws_dynamodb_table.tasks.name}"
    CLUSTER_NAME = "${var.cluster_name}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/deployment_tracking/task_tracking.zip"
}

module "trigger_task_tracking" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name    = "${module.lambda_task_tracking.function_name}"
  lambda_function_arn     = "${module.lambda_task_tracking.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}

module "lambda_dynamo_event_to_sns" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.4"

  name        = "dynamo_event_to_sns_task_tracking"
  module_name = "dynamo_event_to_sns"
  description = "Push new events form DynamoDB updates to SNS"

  environment_variables = {
    INSERT_TOPIC = "${module.task_updates_topic.arn}"
    MODIFY_TOPIC = "${module.task_updates_topic.arn}"
    REMOVE_TOPIC = "${module.task_updates_topic.arn}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/dynamo_event_to_sns.zip"
}

module "trigger_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_dynamo?ref=v1.0.0"

  batch_size = "50"

  stream_arn = "${aws_dynamodb_table.tasks.stream_arn}"

  function_arn  = "${module.lambda_dynamo_event_to_sns.arn}"
  function_role = "${module.lambda_dynamo_event_to_sns.role_name}"
}
