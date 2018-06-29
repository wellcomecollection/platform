data "aws_iam_policy_document" "allow_dynamodb_delete_item" {
  statement {
    actions = [
      "dynamodb:DeleteItem",
    ]

    resources = [
      "${join(",",formatlist("arn:aws:dynamodb:*:*:table/%s", split(",", replace(var.dynamo_table_names, ", ", ","))))}",
    ]
  }
}
