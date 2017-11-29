resource "aws_dynamodb_table" "sierra_table" {
  name             = "SierraData_${var.resource_type}"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "id"
  range_key        = "modifiedDate"
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "modifiedDate"
    type = "N"
  }

  lifecycle {
    prevent_destroy = true

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}
