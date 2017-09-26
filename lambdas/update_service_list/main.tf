module "lambda_update_service_list" {
  source      = "../../terraform/lambda"
  name        = "update_service_list"
  description = "Publish ECS service status summary to S3"
  timeout     = 10

  environment_variables = {
    BUCKET_NAME     = "${var.bucket_dashboard_id}"
    OBJECT_KEY      = "data/ecs_status.json"
    ASSUMABLE_ROLES = "${join(",", var.dashboard_assumable_roles)}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/update_service_list.zip"
}

module "trigger_update_service_list" {
  source = "../../terraform/lambda/trigger_cloudwatch"

  lambda_function_name    = "${module.lambda_update_service_list.function_name}"
  lambda_function_arn     = "${module.lambda_update_service_list.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}
