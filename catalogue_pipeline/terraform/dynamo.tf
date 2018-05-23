resource "aws_dynamodb_table" "matcher_table" {
  name           = "works-graph"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "workId"

  attribute {
    name = "workId"
    type = "S"
  }

  attribute {
    name = "setId"
    type = "S"
  }

  global_secondary_index {
    name            = "work-sets-index"
    hash_key        = "setId"
    write_capacity  = 1
    read_capacity   = 1
    projection_type = "ALL"
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
      "global_secondary_index", # ignore all as Terraform doesn't currently allow ignoring specifically global_secondary_index.write_capacity and global_secondary_index.read_capacity
    ]
  }
}

module "matcher_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=v10.2.0"

  table_name = "${aws_dynamodb_table.matcher_table.name}"

  enable_read_scaling     = true
  read_target_utilization = 70
  read_min_capacity       = 1
  read_max_capacity       = 300

  enable_write_scaling     = true
  write_target_utilization = 70
  write_min_capacity       = 1
  write_max_capacity       = 300
}
