data "aws_iam_policy_document" "dynamodb_delete_item_policy" {
  statement {
    actions = [
      "dynamodb:DeleteItem",
    ]

    resources = [
      "arn:aws:dynamodb:::",
    ]
  }
}
