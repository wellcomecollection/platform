# Lambda for publishing ECS service status summary to S3
module "lambda_update_service_list" {
  source      = "./lambda"
  name        = "update_service_list"
  description = "Publish ECS service status summary to S3"
  source_dir  = "../lambdas/update_service_list"

  environment_variables = {
    BUCKET_NAME = "${aws_s3_bucket.dashboard.id}"
    OBJECT_KEY  = "data/ecs_status.json"
  }
}

module "trigger_update_service_list" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_update_service_list.function_name}"
  lambda_function_arn     = "${module.lambda_update_service_list.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.ecs_task_state_change.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.ecs_task_state_change.name}"
}

# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_service_scheduler" {
  source      = "./lambda"
  name        = "service_scheduler"
  description = "Publish an ECS service schedule to SNS"
  source_dir  = "../lambdas/service_scheduler"
}

module "trigger_calm_adapter" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_service_scheduler.function_name}"
  lambda_function_arn     = "${module.lambda_service_scheduler.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.weekdays_at_7am.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.weekdays_at_7am.name}"

  input = <<EOF
{
  "topic_arn": "${module.service_scheduler_topic.arn}",
  "cluster": "${aws_ecs_cluster.services.name}",
  "service": "${module.calm_adapter.service_name}",
  "desired_count": 1
}
EOF
}

# Lambda for updating ECS service size

module "lambda_update_ecs_service_size" {
  source      = "./lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  source_dir  = "../lambdas/update_ecs_service_size"
}

module "trigger_update_ecs_service_size" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_update_ecs_service_size.function_name}"
  lambda_function_arn  = "${module.lambda_update_ecs_service_size.arn}"
  sns_trigger_arn      = "${module.service_scheduler_topic.arn}"
}

# Lambda for restarting applications when their config changes

module "lambda_update_task_for_config_change" {
  source      = "./lambda"
  name        = "update_task_for_config_change"
  description = "Trigger a task definition change and restart on config change."
  source_dir  = "../lambdas/update_task_for_config_change"
}

module "trigger_application_restart_on_config_change" {
  source               = "./lambda/trigger_s3"
  lambda_function_name = "${module.lambda_update_task_for_config_change.function_name}"
  lambda_function_arn  = "${module.lambda_update_task_for_config_change.arn}"
  s3_bucket_arn        = "${aws_s3_bucket.infra.arn}"
  s3_bucket_id         = "${aws_s3_bucket.infra.id}"
  filter_prefix        = "config/prod/"
  filter_suffix        = ".ini"
}

# Lambda for sheduling the reindexer

module "lambda_schedule_reindexer" {
  source      = "./lambda"
  name        = "schedule_reindexer"
  description = "Schedules the reindexer based on the ReindexerTracker table"
  source_dir  = "../lambdas/schedule_reindexer"

  environment_variables = {
    SCHEDULER_TOPIC_ARN     = "${module.service_scheduler_topic.arn}"
    DYNAMO_TABLE_NAME       = "${aws_dynamodb_table.miro_table.name}"
    DYNAMO_TOPIC_ARN        = "${module.dynamo_capacity_topic.arn}"
    DYNAMO_DESIRED_CAPACITY = "300"
    CLUSTER_NAME            = "${aws_ecs_cluster.services.name}"
    REINDEXERS              = "${aws_dynamodb_table.miro_table.name}=miro_reindexer"
  }
}

module "trigger_reindexer_lambda" {
  source            = "./lambda/trigger_dynamo"
  stream_arn        = "${aws_dynamodb_table.reindex_tracker.stream_arn}"
  function_arn      = "${module.lambda_schedule_reindexer.arn}"
  function_role     = "${module.lambda_schedule_reindexer.role_name}"
  batch_size        = 1
  starting_position = "LATEST"
}

# Lambda for updating the capacity of a DynamoDB table

module "lambda_update_dynamo_capacity" {
  source      = "./lambda"
  name        = "update_dynamo_capacity"
  description = "Update the capacity of a DynamoDB table"
  source_dir  = "../lambdas/update_dynamo_capacity"
}

module "trigger_update_dynamo_capacity" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_update_dynamo_capacity.function_name}"
  lambda_function_arn  = "${module.lambda_update_dynamo_capacity.arn}"
  sns_trigger_arn      = "${module.dynamo_capacity_topic.arn}"
}
