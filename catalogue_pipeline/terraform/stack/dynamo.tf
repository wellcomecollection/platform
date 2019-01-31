resource "aws_dynamodb_table" "matcher_graph_table" {
  name           = "${var.namespace}_works-graph"
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "componentId"
    type = "S"
  }

  billing_mode = "PAY_PER_REQUEST"

  global_secondary_index {
    name            = "work-sets-index"
    hash_key        = "componentId"
    projection_type = "ALL"
  }
}

resource "aws_dynamodb_table" "matcher_lock_table" {
  name           = "${var.namespace}_matcher-lock-table"
  hash_key       = "id"

  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "contextId"
    type = "S"
  }

  global_secondary_index {
    name            = "context-ids-index"
    hash_key        = "contextId"
    projection_type = "ALL"
  }

  ttl {
    attribute_name = "expires"
    enabled        = true
  }
}