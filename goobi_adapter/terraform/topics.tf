module "goobi_bucket_notifications_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "goobi_notifications_topic"
}
