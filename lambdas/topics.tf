module "lambda_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "lambda_error_alarm"
}

module "old_deployments" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "old_deployments"
}

module "dynamo_capacity_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "dynamo_capacity_requests"
}

module "service_scheduler_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "service_scheduler"
}
