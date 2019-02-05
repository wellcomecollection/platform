# Graph table

resource "aws_dynamodb_table" "matcher_graph_table" {
  name     = "${var.namespace}_works-graph"
  hash_key = "id"

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

data "aws_iam_policy_document" "graph_table_readwrite" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:BatchGetItem",
      "dynamodb:GetItem",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_graph_table.arn}",
    ]
  }

  statement {
    actions = [
      "dynamodb:Query",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_graph_table.arn}/index/*",
    ]
  }
}

# Lock table

resource "aws_dynamodb_table" "matcher_lock_table" {
  name     = "${var.namespace}_matcher-lock-table"
  hash_key = "id"

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

data "aws_iam_policy_document" "lock_table_readwrite" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:DeleteItem",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_lock_table.arn}",
    ]
  }

  statement {
    actions = [
      "dynamodb:Query",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_lock_table.arn}/index/*",
    ]
  }
}
