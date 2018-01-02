module "topic_sierra_updates" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_dynamo_updates_${var.resource_type}"
}
