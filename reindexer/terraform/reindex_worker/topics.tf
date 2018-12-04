module "reindex_jobs_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_worker_jobs"
}
