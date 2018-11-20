module "ecs_sqs_freezeray_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sqs_freezeray"
}

module "ecs_sqs_redrive_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sqs_redrive"
}
