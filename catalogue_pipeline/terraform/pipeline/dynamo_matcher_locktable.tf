resource "aws_dynamodb_table" "matcher_lock_table" {
  name           = "${var.namespace}_matcher-lock-table"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "contextId"
    type = "S"
  }

  global_secondary_index {
    name            = "${var.matcher_lock_table_index}"
    hash_key        = "contextId"
    write_capacity  = 1
    read_capacity   = 1
    projection_type = "ALL"
  }

  ttl {
    attribute_name = "expires"
    enabled        = true
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

module "matcher_lock_table_dynamo_autoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/dynamodb?ref=v10.2.0"

  table_name = "${aws_dynamodb_table.matcher_lock_table.name}"

  enable_read_scaling     = true
  read_target_utilization = 30
  read_min_capacity       = 1
  read_max_capacity       = 750

  enable_write_scaling     = true
  write_target_utilization = 30
  write_min_capacity       = 1
  write_max_capacity       = 750
}

module "lambda_dynamodb_write_heartbeat" {
  source = "../../shared_infra/dynamo_write_heartbeat"

  name               = "matcher"
  dynamo_table_names = ["${aws_dynamodb_table.matcher_lock_table.name}"]

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
