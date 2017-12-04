module "sierra_bib_merger_events_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "sierra_bib_merger_events"
}
