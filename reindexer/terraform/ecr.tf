module "ecr_repository_reindex_request_creator" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "reindex_request_creator"
}

module "ecr_repository_reindex_request_processor" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "reindex_request_processor"
}
