module "demultiplexer_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_demultiplexed_${var.resource_type}"
}
