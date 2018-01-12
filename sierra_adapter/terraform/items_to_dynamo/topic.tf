module "sierra_to_dynamo_updates_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_items_to_dynamo_updates"
}