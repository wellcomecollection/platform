module "ecr_repository_sierra_bib_merger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_bib_merger"
}

module "ecr_repository_sierra_item_merger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_item_merger"
}
