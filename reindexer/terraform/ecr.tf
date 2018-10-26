module "ecr_repository_reindex_worker" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "reindex_worker"
}

// TODO: remove when no longer needed
module "ecr_repository_reindex_request_creator" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "reindex_request_creator"
}
