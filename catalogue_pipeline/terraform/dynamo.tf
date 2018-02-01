resource "aws_dynamodb_table" "miro_table" {
  name             = "MiroData"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "MiroID"
  range_key        = "MiroCollection"
  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "MiroID"
    type = "S"
  }

  attribute {
    name = "MiroCollection"
    type = "S"
  }

  attribute {
    name = "ReindexShard"
    type = "S"
  }

  attribute {
    name = "ReindexVersion"
    type = "N"
  }

  global_secondary_index = {
    name            = "ReindexTracker"
    hash_key        = "ReindexShard"
    range_key       = "ReindexVersion"
    read_capacity   = 1
    write_capacity  = 1
    projection_type = "ALL"
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}

resource "aws_dynamodb_table" "reindex_tracker" {
  name             = "ReindexTracker"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "TableName"
  range_key        = "ReindexShard"
  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "TableName"
    type = "S"
  }

  attribute {
    name = "ReindexShard"
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

module "reindexer_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=dynamodb-autoscaling"

  table_name = "${aws_dynamodb_table.reindex_tracker.name}"

  enable_read_scaling     = true
  read_target_utilization = 70
  read_min_capacity       = 1
  read_max_capacity       = 100

  enable_write_scaling     = true
  write_target_utilization = 70
  write_min_capacity       = 1
  write_max_capacity       = 100
}

resource "aws_dynamodb_table" "sourcedata_table" {
  name             = "SourceData"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "sourceId"
  range_key         = "sourceName"
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  attribute {
    name = "sourceId"
    type = "S"
  }

  attribute {
    name = "SourceName"
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

module "sourcedata_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=dynamodb-autoscaling"

  table_name = "${aws_dynamodb_table.sourcedata_table.name}"

  enable_read_scaling     = true
  read_target_utilization = 70
  read_min_capacity       = 1
  read_max_capacity       = 150

  enable_write_scaling     = true
  write_target_utilization = 70
  write_min_capacity       = 1
  write_max_capacity       = 150
}