module "lambda_error_alarm" {
  source = "../terraform/sns"
  name   = "lambda_error_alarm"
}

module "old_deployments" {
  source = "../terraform/sns"
  name   = "old_deployments"
}

module "dynamo_capacity_topic" {
  source = "../terraform/sns"
  name   = "dynamo_capacity_requests"
}

module "service_scheduler_topic" {
  source = "../terraform/sns"
  name   = "service_scheduler"
}
