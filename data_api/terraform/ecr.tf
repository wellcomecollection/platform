module "ecr_repository_snapshot_generator" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "snapshot_generator"
}
