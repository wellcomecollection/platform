# Lambda for publishing out of date deployments to SNS

module "lambda_notify_old_deploys" {
  source      = "./lambda"
  name        = "notify_old_deploys"
  description = "For publishing out of date deployments to SNS"
  source_dir  = "../lambdas/notify_old_deploys"

  environment_variables = {
    TABLE_NAME        = "${aws_dynamodb_table.deployments.name}"
    TOPIC_ARN         = "${module.old_deployments.arn}"
    AGE_BOUNDARY_MINS = "5"
  }
}

module "trigger_notify_old_deploys" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_notify_old_deploys.function_name}"
  lambda_function_arn     = "${module.lambda_notify_old_deploys.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.every_5_minutes.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
}

# Lambda for tracking deployment status in dynamo db
module "lambda_service_deployment_status" {
  source      = "./lambda"
  name        = "service_deployment_status"
  description = "Lambda for tracking deployment status in dynamo db"
  source_dir  = "../lambdas/service_deployment_status"

  environment_variables = {
    TABLE_NAME = "${aws_dynamodb_table.deployments.name}"
  }
}

module "trigger_service_deployment_status" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_service_deployment_status.function_name}"
  lambda_function_arn     = "${module.lambda_service_deployment_status.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
}

# Lambda for tagging EC2 instances with ECS cluster/container instance id
module "lambda_ecs_ec2_instance_tagger" {
  source      = "./lambda"
  name        = "ecs_ec2_instance_tagger"
  description = "Tag an EC2 instance with ECS cluster/container instance id"
  source_dir  = "../lambdas/ecs_ec2_instance_tagger"
}

module "trigger_ecs_ec2_instance_tagger" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_ecs_ec2_instance_tagger.function_name}"
  lambda_function_arn     = "${module.lambda_ecs_ec2_instance_tagger.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
  custom_input            = false
}

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

module "lambda_drain_ecs_container_instance" {
  source      = "./lambda"
  name        = "drain_ecs_container_instance"
  description = "Drain ECS container instance when the corresponding EC2 instance is being terminated"
  source_dir  = "../lambdas/drain_ecs_container_instance"
  timeout     = 60
}

module "trigger_drain_ecs_container_instance" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_drain_ecs_container_instance.function_name}"
  lambda_function_arn  = "${module.lambda_drain_ecs_container_instance.arn}"
  sns_trigger_arn      = "${module.ec2_terminating_topic.arn}"
}

# Lambda for posting on slack when an alarm is triggered

module "lambda_post_to_slack" {
  source      = "./lambda"
  name        = "post_to_slack"
  description = "Post notification to Slack when an alarm is triggered"
  source_dir  = "../lambdas/post_to_slack"

  environment_variables = {
    SLACK_INCOMING_WEBHOOK = "${var.slack_webhook}"
  }
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${module.dlq_alarm.arn}"
}

module "trigger_post_to_slack_esg_not_terminating" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

module "trigger_post_to_slack_server_error_alb" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${module.alb_server_error_alarm.arn}"
}

module "trigger_post_to_slack_client_error_alb" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${module.alb_client_error_alarm.arn}"
}

# Lambda for pushing updates to dynamo tables into sqs queues

module "lambda_dynamo_to_sns" {
  source      = "./lambda"
  name        = "dynamo_to_sns"
  description = ""
  source_dir  = "../lambdas/dynamo_to_sns"

  environment_variables = {
    STREAM_TOPIC_MAP = <<EOF
      {
        "${aws_dynamodb_table.miro_table.stream_arn}": "${module.miro_transformer_topic.arn}",
        "${aws_dynamodb_table.calm_table.stream_arn}": "${module.calm_transformer_topic.arn}"
      }
      EOF
  }
}

module "trigger_dynamo_to_sns_miro" {
  source            = "./lambda/trigger_dynamo"
  stream_arn        = "${aws_dynamodb_table.miro_table.stream_arn}"
  function_arn      = "${module.lambda_dynamo_to_sns.arn}"
  function_role     = "${module.lambda_dynamo_to_sns.role_name}"
  batch_size        = 1
  starting_position = "LATEST"
}

module "trigger_dynamo_to_sns_calm" {
  source            = "./lambda/trigger_dynamo"
  stream_arn        = "${aws_dynamodb_table.calm_table.stream_arn}"
  function_arn      = "${module.lambda_dynamo_to_sns.arn}"
  function_role     = "${module.lambda_dynamo_to_sns.role_name}"
  batch_size        = 1
  starting_position = "LATEST"
}
