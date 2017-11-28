module "ecs_sierra_to_dynamo_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sierra_to_dynamo_${var.resource_type}"
}
