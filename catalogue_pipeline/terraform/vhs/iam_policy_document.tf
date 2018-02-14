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
      "${aws_dynamodb_table.table.arn}",
    ]
  }

  statement {
    actions = [
      "s3:List*",
      "s3:Get*",
    ]

    resources = [
      "${aws_s3_bucket.sierra_data.arn}/",
      "${aws_s3_bucket.sierra_data.arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "full_access_policy" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.table.arn}",
    ]
  }

  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.sierra_data.arn}/",
      "${aws_s3_bucket.sierra_data.arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "dynamo_put_policy" {
  statement {
    actions = [
      "dynamodb:PutItem",
    ]

    resources = [
      "${aws_dynamodb_table.table.arn}",
    ]
  }
}
