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
