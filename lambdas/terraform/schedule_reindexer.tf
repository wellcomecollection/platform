# Lambda for sheduling the reindexer

module "lambda_schedule_reindexer" {
  source      = "./lambda"
  name        = "schedule_reindexer"
  description = "Schedules the reindexer based on the ReindexerTracker table"
  source_dir  = "../src/schedule_reindexer"

  environment_variables = {
    SCHEDULER_TOPIC_ARN     = "${data.terraform_remote_state.platform.service_scheduler_topic_arn}"
    DYNAMO_TABLE_NAME       = "${data.terraform_remote_state.platform.dynamodb_table_miro_table_name}"
    DYNAMO_TOPIC_ARN        = "${data.terraform_remote_state.platform.dynamo_capacity_topic_arn}"
    DYNAMO_DESIRED_CAPACITY = "125"
    CLUSTER_NAME            = "${data.terraform_remote_state.platform.ecs_services_cluster_name}"
    REINDEXERS              = "${data.terraform_remote_state.platform.dynamodb_table_miro_table_name}=miro_reindexer"
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_reindexer_lambda" {
  source        = "./lambda/trigger_dynamo"
  stream_arn    = "${data.terraform_remote_state.platform.dynamodb_table_reindex_tracker_stream_arn}"
  function_arn  = "${module.lambda_schedule_reindexer.arn}"
  function_role = "${module.lambda_schedule_reindexer.role_name}"
}
