module "topic_sierra_windows" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_${var.resource_type}_windows"
}
