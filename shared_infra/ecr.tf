module "ecr_repository_sqs_freezeray" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sqs_freezeray"
}
