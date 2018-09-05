data "aws_dynamodb_table" "storage_manifest" {
  name = "${module.vhs_archive_manifest.table_name}"
}

resource "aws_dynamodb_table" "archive_progress_table" {
  name           = "${local.namespace}-progress-table"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  //  ttl {
  //    attribute_name = "expires"
  //    enabled        = true
  //  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}

module "matcher_lock_table_dynamo_autoscaling" {
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
