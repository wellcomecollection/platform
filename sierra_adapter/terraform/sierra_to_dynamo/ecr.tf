module "ecr_repository" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_${var.resource_type}_to_dynamo"
}
