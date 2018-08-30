module "reindex_jobs_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_jobs-${var.namespace}"
}

module "hybrid_records_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_records_for_reindexing-${var.namespace}"
}
