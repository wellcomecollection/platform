module "drain_ecs_container_instance" {
  source = "./drain_ecs_container_instance"

  ec2_terminating_topic_publish_policy = "${data.terraform_remote_state.platform.ec2_terminating_topic_publish_policy}"
  ec2_terminating_topic_arn            = "${data.terraform_remote_state.platform.ec2_terminating_topic_arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "dynamo_to_sns" {
  source = "./dynamo_to_sns"

  miro_transformer_topic_arn = "${data.terraform_remote_state.platform.miro_transformer_topic_arn}"
  miro_table_stream_arn      = "${data.terraform_remote_state.platform.miro_table_stream_arn}"

  miro_transformer_topic_publish_policy = "${data.terraform_remote_state.platform.miro_transformer_topic_publish_policy}"
  calm_transformer_topic_publish_policy = "${data.terraform_remote_state.platform.calm_transformer_topic_publish_policy}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "ecs_ec2_instance_tagger" {
  source = "./ecs_ec2_instance_tagger"

  bucket_infra_id  = "${data.terraform_remote_state.platform.bucket_infra_id}"
  bucket_infra_arn = "${data.terraform_remote_state.platform.bucket_infra_arn}"

  ecs_container_instance_state_change_name = "${data.terraform_remote_state.platform.ecs_container_instance_state_change_name}"
  ecs_container_instance_state_change_arn  = "${data.terraform_remote_state.platform.ecs_container_instance_state_change_arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "notify_old_deploys" {
  source = "./notify_old_deploys"

  dynamodb_table_deployments_name            = "${aws_dynamodb_table.deployments.name}"
  every_minute_arn                           = "${aws_cloudwatch_event_rule.every_minute.arn}"
  every_minute_name                          = "${aws_cloudwatch_event_rule.every_minute.name}"
  old_deployments_arn                        = "${module.old_deployments.arn}"
  iam_policy_document_deployments_table_json = "${data.aws_iam_policy_document.deployments_table.json}"
  old_deployments_publish_policy             = "${module.old_deployments.publish_policy}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "run_ecs_task" {
  source = "./run_ecs_task"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "schedule_reindexer" {
  source = "./schedule_reindexer"

  dynamodb_table_reindex_tracker_stream_arn = "${data.terraform_remote_state.platform.dynamodb_table_reindex_tracker_stream_arn}"
  ecs_services_cluster_name                 = "${data.terraform_remote_state.platform.ecs_services_cluster_name}"
  dynamodb_table_miro_table_name            = "${data.terraform_remote_state.platform.dynamodb_table_miro_table_name}"

  dynamo_capacity_topic_arn            = "${module.dynamo_capacity_topic.arn}"
  dynamo_capacity_topic_publish_policy = "${module.dynamo_capacity_topic.publish_policy}"

  service_scheduler_topic_arn            = "${module.service_scheduler_topic.arn}"
  service_scheduler_topic_publish_policy = "${module.service_scheduler_topic.publish_policy}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "service_deployment_status" {
  source = "./service_deployment_status"

  dynamodb_table_deployments_name = "${aws_dynamodb_table.deployments.name}"
  every_minute_arn                = "${aws_cloudwatch_event_rule.every_minute.arn}"
  every_minute_name               = "${aws_cloudwatch_event_rule.every_minute.name}"

  iam_policy_document_deployments_table_json = "${data.aws_iam_policy_document.deployments_table.json}"
  iam_policy_document_describe_services_json = "${data.aws_iam_policy_document.describe_services.json}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "service_scheduler" {
  source = "./service_scheduler"

  service_scheduler_topic_publish_policy = "${module.service_scheduler_topic.publish_policy}"
  lambda_error_alarm_arn                 = "${module.lambda_error_alarm.arn}"
}

module "update_dynamo_capacity" {
  source = "./update_dynamo_capacity"

  dynamo_capacity_topic_arn = "${module.dynamo_capacity_topic.arn}"
  ec2_terminating_topic_arn = "${data.terraform_remote_state.platform.ec2_terminating_topic_arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "update_ecs_service_size" {
  source = "./update_ecs_service_size"

  service_scheduler_topic_arn = "${module.service_scheduler_topic.arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

module "update_task_for_config_change" {
  source = "./update_task_for_config_change"

  bucket_infra_arn = "${data.terraform_remote_state.platform.bucket_infra_arn}"
  bucket_infra_id  = "${data.terraform_remote_state.platform.bucket_infra_id}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${data.terraform_remote_state.platform.bucket_infra_id}"

  lambda_function {
    lambda_function_arn = "${module.update_task_for_config_change.lambda_arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "config/prod/"
    filter_suffix       = ".ini"
  }
}
