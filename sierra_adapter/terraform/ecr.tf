module "ecr_repository_sierra_adapter" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "sierra_objects_to_s3-publish"
}
