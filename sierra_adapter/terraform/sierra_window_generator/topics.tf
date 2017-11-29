module "topic_sierra_windows" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_windows_${var.resource_type}"
}
