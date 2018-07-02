data "aws_iam_policy_document" "allow_dynamodb_delete_item" {
  statement {
    actions = [
      "dynamodb:DeleteItem",
    ]

    resources = [
      "${join(",", formatlist("arn:aws:dynamodb:*:*:table/%s", var.dynamo_table_names))}",
    ]
  }
}

# restrict the resource permissions to the specified dynamo tables
# obtained from the dynamo_table_names list:
#
# ["table1", "table2"]
#
# => resources = [ "arn:aws:dynamodb:*:*:table/table1",
#                  "arn:aws:dynamodb:*:*:table/table2"
#                ]

