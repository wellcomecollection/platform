module "snapshot_convertor_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "snapshot_convertor"
}

module "snapshot_conversion_complete_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "snapshot_conversion_complete"
}
