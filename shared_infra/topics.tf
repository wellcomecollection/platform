module "lambda_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "lambda_error_alarm"
}

module "old_deployments" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "old_deployments"
}

# Shared topic for terminating instances

module "ec2_terminating_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_ec2_terminating_topic"
}

# Alarm topics

module "ec2_instance_terminating_for_too_long_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_ec2_instance_terminating_for_too_long_alarm"
}

module "dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_dlq_alarm"
}

module "alb_server_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_alb_server_error_alarm"
}

module "alb_client_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_alb_client_error_alarm"
}

module "terminal_failure_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_terminal_failure_alarm"
}

module "terraform_apply_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "shared_terraform_apply"
}
