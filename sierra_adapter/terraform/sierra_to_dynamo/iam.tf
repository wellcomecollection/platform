module "ecs_sierra_to_dynamo_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sierra_to_dynamo_${var.resource_type}"
}

resource "aws_iam_role_policy" "allow_read_from_windows_queue" {
  role   = "${module.ecs_sierra_to_dynamo_iam.task_role_name}"
  policy = "${module.windows_queue.read_policy}"
}

data "aws_iam_policy_document" "deployments_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.sierra_table.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.ecs_sierra_to_dynamo_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.deployments_table.json}"
}
