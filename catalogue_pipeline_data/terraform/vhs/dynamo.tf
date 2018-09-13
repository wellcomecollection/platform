resource "aws_dynamodb_table" "table" {
  name           = "${local.table_name}"
  count          = "${var.prevent_destroy == "true" ? 1 : 0}"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
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

resource "aws_dynamodb_table" "transient_table" {
  name           = "${local.table_name}"
  count          = "${var.prevent_destroy == "false" ? 1 : 0}"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  lifecycle {
    prevent_destroy = false

    ignore_changes = [
      "read_capacity",
      "write_capacity",
    ]
  }
}
