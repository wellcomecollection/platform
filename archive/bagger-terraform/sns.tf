module "bnumbers_processing_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "archive-bagger_bnumbers_processing"
}
