data "aws_s3_bucket" "assets" {
  bucket = "${var.bucket_name}"
}

data "iam_policy_document" "read_policy" {

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
  }

  resource = [
    "${data.aws_s3_bucket.assets.arn}/${var.name}/",
    "${data.aws_s3_bucket.assets.arn}/${var.name}/*",
  ]
}

data "iam_policy_document" "full_access_policy" {

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
  }

  resource = [
    "${data.aws_s3_bucket.assets.arn}/${var.name}/",
    "${data.aws_s3_bucket.assets.arn}/${var.name}/*",
  ]
}

