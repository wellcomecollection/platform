module "reindex_shard_tracker_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "reindex_shard_tracker_updates"
}
