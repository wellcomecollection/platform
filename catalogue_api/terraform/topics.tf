module "ec2_terminating_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "ec2_terminating_topic"
}

# Alarm topics

module "ec2_instance_terminating_for_too_long_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "ec2_instance_terminating_for_too_long_alarm"
}

module "dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "dlq_alarm"
}

module "alb_server_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "alb_server_error_alarm"
}

module "alb_client_error_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "alb_client_error_alarm"
}

module "terminal_failure_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "terminal_failure_alarm"
}

module "terraform_apply_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "terraform_apply"
}
