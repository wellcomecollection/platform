resource "aws_dynamodb_table" "table" {
  name             = "${var.table_name}"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "id"
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "reindexShard"
    type = "S"
  }

  attribute {
    name = "reindexVersion"
    type = "N"
  }

  global_secondary_index {
    name            = "reindexTracker"
    hash_key        = "reindexShard"
    range_key       = "reindexVersion"
    write_capacity  = 1
    read_capacity   = 1
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

module "sourcedata_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=dynamodb-autoscaling"

  table_name = "${aws_dynamodb_table.table.name}"

  enable_read_scaling     = true
  read_target_utilization = 70
  read_min_capacity       = 1
  read_max_capacity       = 300

  enable_write_scaling     = true
  write_target_utilization = 70
  write_min_capacity       = 1
  write_max_capacity       = 300
}
