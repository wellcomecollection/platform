module "ecr_repository_sierra_to_dynamo" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_items_to_dynamo"
}
