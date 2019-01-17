module "transformed_miro_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_transformed_miro_works"
}

module "transformed_sierra_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_transformed_sierra_works"
}
