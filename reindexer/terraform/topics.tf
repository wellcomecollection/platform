module "reindex_jobs_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_jobs"
}

module "reindex_jobs_complete_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v6.4.2"
  name   = "reindex_jobs_complete"
}

module "reindex_shard_tracker_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_shard_tracker_updates"
}
