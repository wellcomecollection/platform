module "ecr_repository_goobi_reader" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "goobi_reader"
}
