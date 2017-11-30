module "ecr_repository_sierra_to_dynamo" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_to_dynamo"
}

module "ecr_repository_sierra_bib_merger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_bib_merger"
}
