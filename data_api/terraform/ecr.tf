module "ecr_repository_snapshot_convertor" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "snapshot_convertor"
}
