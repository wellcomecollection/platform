module "snapshot_generator_jobs_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "snapshot_generator_jobs"
}

module "snapshot_generation_complete_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "snapshot_generation_complete"
}
