data "template_file" "table_names" {
  count = "${length(var.heartbeat_config)}"
  template = "${lookup(var.heartbeat_config[count.index], "table_name")}"
}

data "aws_iam_policy_document" "allow_dynamodb_delete_item" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
    ]

    resources = [
      "${join(",", formatlist("arn:aws:dynamodb:*:*:table/%s", data.template_file.table_names.*.rendered))}",
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

