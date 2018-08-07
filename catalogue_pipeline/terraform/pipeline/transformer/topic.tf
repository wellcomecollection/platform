module "transformer_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_${var.source_name}_transformer"
}
