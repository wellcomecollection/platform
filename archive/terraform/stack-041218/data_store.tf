resource "aws_dynamodb_table" "archive_progress_table" {
  name           = "${var.namespace}-progress-table"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "bagIdIndex"
    type = "S"
  }

  attribute {
    name = "createdDate"
    type = "S"
  }

  global_secondary_index {
    name            = "${var.namespace}-bag-progress-index"
    hash_key        = "bagIdIndex"
    range_key       = "createdDate"
    write_capacity  = 1
    read_capacity   = 1
    projection_type = "INCLUDE"
    non_key_attributes = ["bagIdIndex", "id", "createdDate"]
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}

module "archive_progress_table_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=v10.2.0"

  table_name = "${aws_dynamodb_table.archive_progress_table.name}"

  enable_read_scaling     = true
  read_target_utilization = 30
  read_min_capacity       = 1
  read_max_capacity       = 10

  enable_write_scaling     = true
  write_target_utilization = 30
  write_min_capacity       = 1
  write_max_capacity       = 10
}
