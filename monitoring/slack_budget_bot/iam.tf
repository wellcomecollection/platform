data "aws_s3_bucket" "dashboard" {
  bucket = "${var.dashboard_bucket_id}"
}

data "aws_iam_policy_document" "allow_s3_write" {
  statement {
    actions = [
      "s3:Put*",
    ]

    resources = [
      "${data.aws_s3_bucket.dashboard.arn}/budget_graphs/*",
    ]
  }
}

resource "aws_iam_role_policy" "allow_s3_write" {
  role   = "${module.ecs_slack_budget_bot_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_write.json}"
}

data "aws_iam_policy_document" "allow_describe_budgets" {
  statement {
    actions = [
      "budgets:ViewBudget",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "allow_describe_budgets" {
  role   = "${module.ecs_slack_budget_bot_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_describe_budgets.json}"
}
