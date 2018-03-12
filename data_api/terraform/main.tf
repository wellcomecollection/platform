# module "elasticdump" {
#   source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
#   task_name     = "elasticdump"
#   app_uri       = "wellcome/elasticdump:latest"
#   task_role_arn = "${module.ecs_elasticdump_iam.task_role_arn}"
#
#   env_vars = [
#     "{\"name\": \"BUCKET\", \"value\": \"${var.elasticdump_config_bucket}\"}",
#     "{\"name\": \"S3_KEY\", \"value\": \"${var.elasticdump_config_s3_key}\"}",
#   ]
# }

module "snapshot_scheduler" {
  source = "snapshot_scheduler"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}
