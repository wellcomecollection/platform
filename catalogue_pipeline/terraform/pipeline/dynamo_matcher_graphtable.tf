resource "aws_dynamodb_table" "matcher_graph_table" {
  name           = "${var.namespace}_works-graph"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "componentId"
    type = "S"
  }

  global_secondary_index {
    name            = "${var.matcher_graph_table_index}"
    hash_key        = "componentId"
    write_capacity  = 1
    read_capacity   = 1
    projection_type = "ALL"
  }

  lifecycle {
    ignore_changes = [
      "read_capacity",
      "write_capacity",
      "global_secondary_index", # ignore all as Terraform doesn't currently allow ignoring specifically global_secondary_index.write_capacity and global_secondary_index.read_capacity
    ]
  }
}

module "matcher_graph_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=v11.8.0"

  table_name = "${aws_dynamodb_table.matcher_graph_table.name}"

  enable_read_scaling     = true
  read_target_utilization = 30
  read_min_capacity       = 1
  read_max_capacity       = 750

  enable_write_scaling     = true
  write_target_utilization = 30
  write_min_capacity       = 1
  write_max_capacity       = 750

  index_name = "${var.matcher_graph_table_index}"
}
