resource "aws_dynamodb_table" "reindex_shard_tracker" {
  name           = "ReindexShardTracker"
  read_capacity  = 1
  write_capacity = 1

  hash_key = "shardId"

  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "shardId"
    type = "S"
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}

module "reindex_shard_tracker_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=dynamodb-autoscaling"

  table_name = "${aws_dynamodb_table.reindex_shard_tracker.name}"

  enable_read_scaling     = true
  read_target_utilization = 50
  read_min_capacity       = 1
  read_max_capacity       = 20

  enable_write_scaling     = true
  write_target_utilization = 50
  write_min_capacity       = 1
  write_max_capacity       = 20
}
