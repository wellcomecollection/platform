data "aws_iam_policy_document" "read_policy" {
  # This is based on the AmazonDynamoDBReadOnlyAccess
  statement {
    actions = [
      "dynamodb:BatchGetItem",
      "dynamodb:DescribeTable",
      "dynamodb:GetItem",
      "dynamodb:ListTables",
      "dynamodb:Query",
      "dynamodb:Scan",
    ]

    resources = [
      "${local.table_arn}",
    ]
  }

  statement {
    actions = [
      "s3:List*",
      "s3:Get*",
    ]

    resources = [
      "${local.bucket_arn}/",
      "${local.bucket_arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "full_access_policy" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${local.table_arn}",

      # Allow access to the GSIs on the table
      "${local.table_arn}/*",
    ]
  }

  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${local.bucket_arn}/",
      "${local.bucket_arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "dynamodb_update_policy" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
    ]

    resources = [
      "${local.table_arn}",
    ]
  }
}
